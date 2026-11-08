import { useId } from 'react';

// Label + input + hint + error message, wired together so screen readers announce them.
function FieldWrap({ id, label, required, error, hint, children }) {
  return (
    <div className={`field${error ? ' field-has-error' : ''}`}>
      <label htmlFor={id}>
        {label}
        {required && <span className="required" aria-hidden="true"> *</span>}
      </label>
      {children}
      {hint && !error && <p className="field-hint" id={`${id}-hint`}>{hint}</p>}
      {error && <p className="field-error" id={`${id}-error`} role="alert">{error}</p>}
    </div>
  );
}

function aria(id, error, hint) {
  return {
    'aria-invalid': error ? 'true' : undefined,
    'aria-describedby': error ? `${id}-error` : hint ? `${id}-hint` : undefined,
  };
}

export function TextField({ label, error, hint, required, ...inputProps }) {
  const id = useId();
  return (
    <FieldWrap id={id} label={label} required={required} error={error} hint={hint}>
      <input id={id} required={required} {...aria(id, error, hint)} {...inputProps} />
    </FieldWrap>
  );
}

export function TextAreaField({ label, error, hint, required, ...props }) {
  const id = useId();
  return (
    <FieldWrap id={id} label={label} required={required} error={error} hint={hint}>
      <textarea id={id} rows={3} {...aria(id, error, hint)} {...props} />
    </FieldWrap>
  );
}

export function SelectField({ label, error, hint, required, options, placeholder, ...props }) {
  const id = useId();
  return (
    <FieldWrap id={id} label={label} required={required} error={error} hint={hint}>
      <select id={id} {...aria(id, error, hint)} {...props}>
        {placeholder !== undefined && <option value="">{placeholder}</option>}
        {options.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
    </FieldWrap>
  );
}
