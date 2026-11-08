import { useState } from 'react';
import { api } from '../api/endpoints.js';
import { TextField } from './Field.jsx';
import { Info } from './Notice.jsx';
import { hasErrors, validateStockMovement } from '../utils/validation.js';
import { todayIso } from '../utils/format.js';

// Adding stock needs the server (it is not queued offline in this MVP), so errors are shown right here.
export default function StockInForm({ item, onSaved }) {
  const [values, setValues] = useState({ quantity: '', movementDate: todayIso(), notes: '' });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState(null);
  const [saving, setSaving] = useState(false);

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }));

  async function handleSubmit(e) {
    e.preventDefault();
    setMessage(null);
    const found = validateStockMovement(values, { kind: 'stock-in', item, today: todayIso() });
    setErrors(found);
    if (hasErrors(found)) return;
    setSaving(true);
    try {
      await api.feed.stockIn(item.id, {
        quantity: Number(values.quantity),
        movementDate: values.movementDate,
        notes: values.notes.trim() || null,
      });
      setValues((v) => ({ ...v, quantity: '', notes: '' }));
      setMessage({ tone: 'success', text: 'Stock added.' });
      onSaved();
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setMessage({
        tone: 'error',
        text: err.isNetworkError ? 'Adding stock needs a connection to the server. Please try again when it is reachable.' : err.message,
      });
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="card form" onSubmit={handleSubmit} noValidate>
      <h3>Add stock: {item.name}</h3>
      <div className="form-grid">
        <TextField label="Quantity received (kg)" type="number" inputMode="decimal" min="0.001" step="0.001" required value={values.quantity} onChange={set('quantity')} error={errors.quantity} />
        <TextField label="Date" type="date" value={values.movementDate} onChange={set('movementDate')} error={errors.movementDate} max={todayIso()} />
        <TextField label="Notes (optional)" value={values.notes} onChange={set('notes')} maxLength={500} />
      </div>
      {message && <Info tone={message.tone}>{message.text}</Info>}
      <div className="btn-row">
        <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Add stock'}</button>
      </div>
    </form>
  );
}
