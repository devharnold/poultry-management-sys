import { useState } from 'react';
import { api } from '../api/endpoints.js';
import { SelectField, TextAreaField, TextField } from './Field.jsx';
import { Info } from './Notice.jsx';
import { hasErrors, validateFlock } from '../utils/validation.js';
import { todayIso } from '../utils/format.js';

export default function FlockForm({ flock, farms, onSaved, onCancel }) {
  const [values, setValues] = useState({
    flockCode: flock?.flockCode ?? '',
    farmId: flock ? String(flock.farmId) : '',
    birdType: flock?.birdType ?? '',
    placementDate: flock?.placementDate ?? todayIso(),
    initialBirdCount: flock ? String(flock.initialBirdCount) : '',
    status: flock?.status ?? 'ACTIVE',
    notes: flock?.notes ?? '',
  });
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [saving, setSaving] = useState(false);

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }));

  async function handleSubmit(e) {
    e.preventDefault();
    const found = validateFlock(values, todayIso());
    setErrors(found);
    if (hasErrors(found)) return;
    const body = {
      flockCode: values.flockCode.trim(),
      farmId: Number(values.farmId),
      birdType: values.birdType.trim(),
      placementDate: values.placementDate,
      initialBirdCount: Number(values.initialBirdCount),
      status: values.status,
      notes: values.notes,
    };
    setSaving(true);
    setSubmitError(null);
    try {
      const saved = flock ? await api.flocks.update(flock.id, body) : await api.flocks.create(body);
      onSaved(saved);
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setSubmitError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="card form" onSubmit={handleSubmit} noValidate>
      <h3>{flock ? `Edit flock ${flock.flockCode}` : 'Add a flock'}</h3>
      <div className="form-grid">
        <TextField label="Flock / batch code" required value={values.flockCode} onChange={set('flockCode')} error={errors.flockCode} maxLength={40} hint="Must be unique, e.g. BR-2026-01" />
        <SelectField
          label="Farm" required value={values.farmId} onChange={set('farmId')} error={errors.farmId}
          placeholder="Choose a farm…" options={farms.map((f) => ({ value: String(f.id), label: f.name }))}
        />
        <TextField label="Bird type / breed" required value={values.birdType} onChange={set('birdType')} error={errors.birdType} maxLength={80} hint="e.g. Broiler, Layer, Kenbro" />
        <TextField label="Placement date" type="date" required value={values.placementDate} onChange={set('placementDate')} error={errors.placementDate} max={todayIso()} />
        <TextField label="Initial bird count" type="number" inputMode="numeric" min="1" step="1" required value={values.initialBirdCount} onChange={set('initialBirdCount')} error={errors.initialBirdCount} />
        <SelectField
          label="Status" value={values.status} onChange={set('status')} error={errors.status}
          options={[{ value: 'ACTIVE', label: 'Active' }, { value: 'COMPLETED', label: 'Completed' }]}
          hint="Mortality can only be recorded for active flocks."
        />
      </div>
      <TextAreaField label="Notes" value={values.notes} onChange={set('notes')} error={errors.notes} maxLength={1000} />
      {submitError && <Info tone="error">{submitError}</Info>}
      <div className="btn-row">
        <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save flock'}</button>
        <button type="button" className="btn" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}
