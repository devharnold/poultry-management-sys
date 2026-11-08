import { useState } from 'react';
import { SelectField, TextField } from './Field.jsx';
import { Info } from './Notice.jsx';
import { useOutbox } from '../hooks/useOutbox.jsx';
import { hasErrors, validateMortality } from '../utils/validation.js';
import { formatDate, todayIso } from '../utils/format.js';

// Works offline: the entry is saved on this device first, then sent when the server can be reached.
export default function MortalityForm({ flocks, defaultFlockId = '' }) {
  const { enqueue } = useOutbox();
  const [values, setValues] = useState({ flockId: defaultFlockId ? String(defaultFlockId) : '', recordDate: todayIso(), count: '', reason: '' });
  const [errors, setErrors] = useState({});
  const [result, setResult] = useState(null);
  const [saving, setSaving] = useState(false);

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }));
  const selected = flocks.find((f) => String(f.id) === values.flockId);

  async function handleSubmit(e) {
    e.preventDefault();
    setResult(null);
    const found = validateMortality(values, selected, todayIso());
    setErrors(found);
    if (hasErrors(found)) return;
    setSaving(true);
    try {
      const label = `Mortality: ${selected.flockCode} · ${values.count} bird(s) · ${formatDate(values.recordDate)}`;
      const entry = await enqueue(
        'mortality',
        { flockId: Number(values.flockId), recordDate: values.recordDate, count: Number(values.count), reason: values.reason.trim() || null },
        label,
      );
      setResult(entry);
      if (entry.status === 'failed') {
        setErrors(entry.fieldErrors || {});
      } else {
        setValues((v) => ({ ...v, count: '', reason: '' }));
        setErrors({});
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="card form" onSubmit={handleSubmit} noValidate>
      <h3>Record mortality</h3>
      <div className="form-grid">
        <SelectField
          label="Flock" required value={values.flockId} onChange={set('flockId')} error={errors.flockId}
          placeholder="Choose a flock…"
          options={flocks.map((f) => ({ value: String(f.id), label: `${f.flockCode} · ${f.farmName} · ${f.currentBirdCount} alive` }))}
        />
        <TextField label="Date" type="date" required value={values.recordDate} onChange={set('recordDate')} error={errors.recordDate} max={todayIso()} />
        <TextField label="Number of deaths" type="number" inputMode="numeric" min="1" step="1" required value={values.count} onChange={set('count')} error={errors.count} />
        <TextField label="Reason / notes (optional)" value={values.reason} onChange={set('reason')} error={errors.reason} maxLength={500} />
      </div>
      {result && result.status === 'synced' && <Info tone="success">Saved on the server. Thank you.</Info>}
      {result && result.status === 'pending' && (
        <Info tone="warning">
          <strong>Pending sync.</strong> The server could not be reached, so this entry is saved on this device and will be sent
          automatically when the connection returns. It is listed below and on the Sync page.
        </Info>
      )}
      {result && result.status === 'failed' && (
        <Info tone="error">
          <strong>Sync failed: the server did not accept this entry.</strong> {result.lastError} Correct the form and submit again; the
          rejected entry stays listed below until you discard it.
        </Info>
      )}
      <div className="btn-row">
        <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save mortality'}</button>
      </div>
    </form>
  );
}
