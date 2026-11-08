import { useState } from 'react';
import { api } from '../api/endpoints.js';
import { TextField } from './Field.jsx';
import { Info } from './Notice.jsx';
import { hasErrors, validateWeight } from '../utils/validation.js';
import { todayIso } from '../utils/format.js';

export default function WeightForm({ flock, onSaved }) {
  const [values, setValues] = useState({ recordDate: todayIso(), averageBirdWeightKg: '', sampledBirdCount: '', notes: '' });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState(null);
  const [saving, setSaving] = useState(false);

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }));

  async function handleSubmit(e) {
    e.preventDefault();
    setMessage(null);
    const found = validateWeight(values, todayIso());
    setErrors(found);
    if (hasErrors(found)) return;
    setSaving(true);
    try {
      await api.flocks.addWeight(flock.id, {
        recordDate: values.recordDate,
        averageBirdWeightKg: Number(values.averageBirdWeightKg),
        sampledBirdCount: Number(values.sampledBirdCount),
        notes: values.notes.trim() || null,
      });
      setValues((v) => ({ ...v, averageBirdWeightKg: '', sampledBirdCount: '', notes: '' }));
      setMessage({ tone: 'success', text: 'Weight recorded.' });
      onSaved();
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setMessage({ tone: 'error', text: err.message });
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="card form" onSubmit={handleSubmit} noValidate>
      <h3>Record an average bird weight</h3>
      <p className="muted small">Weigh a sample of birds and enter the average. For feed conversion, weigh a sample on the placement date too.</p>
      <div className="form-grid">
        <TextField label="Date" type="date" required value={values.recordDate} onChange={set('recordDate')} error={errors.recordDate} max={todayIso()} />
        <TextField label="Average weight per bird (kg)" type="number" inputMode="decimal" min="0.001" step="0.001" required value={values.averageBirdWeightKg} onChange={set('averageBirdWeightKg')} error={errors.averageBirdWeightKg} />
        <TextField label="Birds weighed (sample size)" type="number" inputMode="numeric" min="1" step="1" required value={values.sampledBirdCount} onChange={set('sampledBirdCount')} error={errors.sampledBirdCount} />
        <TextField label="Notes (optional)" value={values.notes} onChange={set('notes')} maxLength={500} />
      </div>
      {message && <Info tone={message.tone}>{message.text}</Info>}
      <div className="btn-row">
        <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save weight'}</button>
      </div>
    </form>
  );
}
