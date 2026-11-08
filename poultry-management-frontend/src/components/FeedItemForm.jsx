import { useState } from 'react';
import { api } from '../api/endpoints.js';
import { TextAreaField, TextField } from './Field.jsx';
import { Info } from './Notice.jsx';
import { hasErrors, validateFeedItem } from '../utils/validation.js';

export default function FeedItemForm({ item, onSaved, onCancel }) {
  const creating = !item;
  const [values, setValues] = useState({
    name: item?.name ?? '',
    feedType: item?.feedType ?? '',
    reorderThreshold: item ? String(item.reorderThreshold) : '',
    openingQuantity: '',
    supplier: item?.supplier ?? '',
    notes: item?.notes ?? '',
  });
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [saving, setSaving] = useState(false);

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }));

  async function handleSubmit(e) {
    e.preventDefault();
    const found = validateFeedItem(values, { creating });
    setErrors(found);
    if (hasErrors(found)) return;
    const body = {
      name: values.name.trim(),
      feedType: values.feedType.trim(),
      reorderThreshold: Number(values.reorderThreshold),
      openingQuantity: creating && values.openingQuantity !== '' ? Number(values.openingQuantity) : null,
      supplier: values.supplier,
      notes: values.notes,
    };
    setSaving(true);
    setSubmitError(null);
    try {
      const saved = creating ? await api.feed.create(body) : await api.feed.update(item.id, body);
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
      <h3>{creating ? 'Add a feed item' : `Edit feed item: ${item.name}`}</h3>
      <div className="form-grid">
        <TextField label="Feed name" required value={values.name} onChange={set('name')} error={errors.name} maxLength={120} />
        <TextField label="Feed type" required value={values.feedType} onChange={set('feedType')} error={errors.feedType} maxLength={80} hint="e.g. Starter, Grower, Layer mash" />
        <TextField label="Reorder threshold (kg)" type="number" inputMode="decimal" min="0" step="0.001" required value={values.reorderThreshold} onChange={set('reorderThreshold')} error={errors.reorderThreshold} hint="A low-stock alert is raised at or below this amount." />
        {creating && (
          <TextField label="Opening stock (kg)" type="number" inputMode="decimal" min="0" step="0.001" value={values.openingQuantity} onChange={set('openingQuantity')} error={errors.openingQuantity} hint="Optional. Recorded as the first stock-in." />
        )}
        <TextField label="Supplier (optional)" value={values.supplier} onChange={set('supplier')} error={errors.supplier} maxLength={120} />
      </div>
      <TextAreaField label="Notes" value={values.notes} onChange={set('notes')} error={errors.notes} maxLength={1000} />
      {!creating && <p className="muted small">The quantity in stock cannot be edited here. Use “Add stock” or “Record usage” so the history stays correct.</p>}
      {submitError && <Info tone="error">{submitError}</Info>}
      <div className="btn-row">
        <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save feed item'}</button>
        <button type="button" className="btn" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}
