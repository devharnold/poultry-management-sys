// Small building blocks for "loading", "error", "empty" and "info" messages.
export function Loading({ text = 'Loading…' }) {
  return (
    <p className="notice notice-info" role="status">
      <span className="spinner" aria-hidden="true" /> {text}
    </p>
  );
}

export function ErrorNotice({ error, onRetry }) {
  if (!error) return null;
  return (
    <div className="notice notice-error" role="alert">
      <strong>Something went wrong.</strong> {error.message || String(error)}
      {onRetry && (
        <div>
          <button type="button" className="btn btn-small" onClick={onRetry}>Try again</button>
        </div>
      )}
    </div>
  );
}

export function Empty({ title, children }) {
  return (
    <div className="empty">
      <p className="empty-title">{title}</p>
      {children && <div className="empty-body">{children}</div>}
    </div>
  );
}

export function Info({ children, tone = 'info' }) {
  return (
    <p className={`notice notice-${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      {children}
    </p>
  );
}
