import { OUTBOX_STATUS_LABEL, formatDateTime } from '../utils/format.js';
import { useOutbox } from '../hooks/useOutbox.jsx';

// Shows offline entries with their status in WORDS: Pending sync / Synced / Sync failed.
export default function OutboxList({ entries, showActions = false }) {
  const { retry, discard } = useOutbox();
  if (entries.length === 0) return null;

  function handleDiscard(entry) {
    const ok = window.confirm(
      `Discard this entry?\n\n"${entry.label}"\n\nIt has not been saved on the server and will be lost for good.`,
    );
    if (ok) discard(entry.id);
  }

  return (
    <ul className="outbox-list">
      {entries.map((entry) => (
        <li key={entry.id} className={`outbox-item outbox-${entry.status}`}>
          <div className="outbox-head">
            <span className={`badge badge-${entry.status}`}>{OUTBOX_STATUS_LABEL[entry.status]}</span>
            <strong>{entry.label}</strong>
          </div>
          <p className="muted small">
            Entered {formatDateTime(entry.createdAt)}
            {entry.attempts > 0 ? ` · ${entry.attempts} attempt${entry.attempts === 1 ? '' : 's'}` : ''}
            {entry.syncedAt ? ` · confirmed ${formatDateTime(entry.syncedAt)}` : ''}
          </p>
          {entry.status === 'pending' && entry.lastError && (
            <p className="small">Waiting to send. Last problem: {entry.lastError}</p>
          )}
          {entry.status === 'failed' && (
            <p className="small field-error" role="alert">The server did not accept this entry: {entry.lastError}</p>
          )}
          {showActions && entry.status !== 'synced' && (
            <div className="btn-row">
              {entry.status === 'failed' && (
                <button type="button" className="btn btn-small" onClick={() => retry(entry.id)}>Try again</button>
              )}
              <button type="button" className="btn btn-small btn-danger" onClick={() => handleDiscard(entry)}>Discard…</button>
            </div>
          )}
        </li>
      ))}
    </ul>
  );
}
