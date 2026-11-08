import { Link } from 'react-router-dom';
import { useConnectivity } from '../hooks/useConnectivity.jsx';
import { useOutbox } from '../hooks/useOutbox.jsx';

// Always-visible status: is the BACKEND reachable, and how many entries are waiting / rejected.
export default function SyncBadge() {
  const { apiReachable } = useConnectivity();
  const { pendingCount, failedCount, syncing } = useOutbox();

  let text = 'Checking server…';
  let tone = 'neutral';
  if (apiReachable === true) { text = 'Server connected'; tone = 'ok'; }
  if (apiReachable === false) { text = 'Server unreachable: entries are saved on this device'; tone = 'bad'; }

  return (
    <Link to="/sync" className={`sync-badge sync-${tone}`} role="status" aria-live="polite">
      <span className="dot" aria-hidden="true" />
      <span>{text}</span>
      {syncing && <span className="pill">syncing…</span>}
      {pendingCount > 0 && <span className="pill pill-warn">{pendingCount} pending sync</span>}
      {failedCount > 0 && <span className="pill pill-bad">{failedCount} sync failed</span>}
    </Link>
  );
}
