import { useState } from 'react';
import { SelectField, TextField } from './Field.jsx';
import { Info } from './Notice.jsx';
import { useOutbox } from '../hooks/useOutbox.jsx';
import { hasErrors, validateStockMovement } from '../utils/validation.js';
import { formatDate, formatKg, todayIso } from '../utils/format.js';

// Feed usage works offline, like mortality. `item` is the selected feed item.
export default function FeedUsageForm({ item, flocks }) {
  const { enqueue } = useOutbox();
  const [values, setValues] = useState({ quantity: '', movementDate: todayIso(), flockId: '', notes: '' });
  const [errors, setErrors] = useState({});
  const [result, setResult] = useState(null);
  const [saving, setSaving] = useState(false);

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }));

  async function handleSubmit(e) {
    e.preventDefault();
    setResult(null);
    const found = validateStockMovement(values, { kind: 'usage', item, today: todayIso() });
    setErrors(found);
    if (hasErrors(found)) return;
    setSaving(true);
    try {
      const flock = flocks.find((f) => String(f.id) === values.flockId);
      const label = `Feed used: ${item.name} · ${values.quantity} kg${flock ? ` · ${flock.flockCode}` : ''} · ${formatDate(values.movementDate)}`;
      const entry = await enqueue(
        'feed-usage',
        {
          feedItemId: item.id,
          quantity: Number(values.quantity),
          movementDate: values.movementDate,
          flockId: values.flockId ? Number(values.flockId) : null,
          notes: values.notes.trim() || null,
        },
        label,
      );
      setResult(entry);
      if (entry.status === 'failed') {
        setErrors(entry.fieldErrors || {});
      } else {
        setValues((v) => ({ ...v, quantity: '', notes: '' }));
        setErrors({});
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="card form" onSubmit={handleSubmit} noValidate>
      <h3>Record feed usage: {item.name}</h3>
      <p className="muted small">In stock (as last loaded): {formatKg(item.quantityOnHand)}</p>
      <div className="form-grid">
        <TextField label="Quantity used (kg)" type="number" inputMode="decimal" min="0.001" step="0.001" required value={values.quantity} onChange={set('quantity')} error={errors.quantity} />
        <TextField label="Date" type="date" value={values.movementDate} onChange={set('movementDate')} error={errors.movementDate} max={todayIso()} />
        <SelectField
          label="Flock that used it" value={values.flockId} onChange={set('flockId')} error={errors.flockId}
          placeholder="Not specified" options={flocks.map((f) => ({ value: String(f.id), label: `${f.flockCode} · ${f.farmName}` }))}
          hint="Needed for that flock's feed conversion ratio."
        />
        <TextField label="Notes (optional)" value={values.notes} onChange={set('notes')} maxLength={500} />
      </div>
      {result && result.status === 'synced' && <Info tone="success">Saved on the server.</Info>}
      {result && result.status === 'pending' && (
        <Info tone="warning"><strong>Pending sync.</strong> Saved on this device; it will be sent when the server can be reached.</Info>
      )}
      {result && result.status === 'failed' && (
        <Info tone="error"><strong>Sync failed: the server did not accept this entry.</strong> {result.lastError}</Info>
      )}
      <div className="btn-row">
        <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save usage'}</button>
      </div>
    </form>
  );
}
