import { useState } from 'react';
import { api } from '../api/endpoints.js';
import { TextAreaField, TextField } from './Field.jsx';
import { Info } from './Notice.jsx';
import { hasErrors, validateFarm } from '../utils/validation.js';

export default function FarmForm({ farm, onSaved, onCancel }) {
  const [values, setValues] = useState({
    name: farm?.name ?? '',
    location: farm?.location ?? '',
    contactNotes: farm?.contactNotes ?? '',
  });
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [saving, setSaving] = useState(false);

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }));

  async function handleSubmit(e) {
    e.preventDefault();
    const found = validateFarm(values);
    setErrors(found);
    if (hasErrors(found)) return;
    setSaving(true);
    setSubmitError(null);
    try {
      const saved = farm ? await api.farms.update(farm.id, values) : await api.farms.create(values);
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
      <h3>{farm ? `Edit farm: ${farm.name}` : 'Add a farm'}</h3>
      <TextField label="Farm name" required value={values.name} onChange={set('name')} error={errors.name} maxLength={120} />
      <TextField label="Location" value={values.location} onChange={set('location')} error={errors.location} maxLength={200} />
      <TextAreaField label="Contact / notes" value={values.contactNotes} onChange={set('contactNotes')} error={errors.contactNotes} maxLength={1000} />
      {submitError && <Info tone="error">{submitError}</Info>}
      <div className="btn-row">
        <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save farm'}</button>
        <button type="button" className="btn" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}
