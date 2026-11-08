import { useConnectivity } from '../hooks/useConnectivity.jsx';
import { useOutbox } from '../hooks/useOutbox.jsx';
import OutboxList from '../components/OutboxList.jsx';
import { Empty, Info } from '../components/Notice.jsx';

export default function SyncPage() {
  const { apiReachable, check } = useConnectivity();
  const { entries, syncNow, syncing, clearSynced, storageKind, persistent } = useOutbox();

  const failed = entries.filter((e) => e.status === 'failed');
  const pending = entries.filter((e) => e.status === 'pending');
  const synced = entries.filter((e) => e.status === 'synced');

  return (
    <section>
      <h2>Sync</h2>
      <p>
        Mortality and feed-usage entries are saved on <strong>this device first</strong> and then sent to the server. If the server cannot be
        reached they wait here as <em>Pending sync</em> and are sent automatically, in order, when it comes back. Each entry carries its own
        id, so sending it twice never creates a second record.
      </p>

      <div className="card">
        <p>
          <strong>Server:</strong>{' '}
          {apiReachable === null ? 'checking…' : apiReachable ? 'reachable' : 'not reachable'} ·{' '}
          <strong>Entries stored with:</strong> {storageKind}
        </p>
        {!persistent && (
          <Info tone="error">
            This browser could not provide permanent storage, so entries are kept in memory only and will be lost if you close or reload
            this tab. Sync before closing.
          </Info>
        )}
        <div className="btn-row">
          <button type="button" className="btn btn-primary" onClick={() => { check(); syncNow(); }} disabled={syncing}>
            {syncing ? 'Syncing…' : 'Check connection and sync now'}
          </button>
          {synced.length > 0 && <button type="button" className="btn" onClick={clearSynced}>Clear list of synced entries</button>}
        </div>
      </div>

      {entries.length === 0 && (
        <Empty title="Nothing waiting">
          <p>Entries you make on the Mortality and Feed pages appear here with their sync status.</p>
        </Empty>
      )}

      {failed.length > 0 && (
        <div>
          <h3>Sync failed: needs your attention ({failed.length})</h3>
          <p className="muted small">The server understood these entries but refused them (for example, more deaths than birds alive). They are kept until you retry or discard them.</p>
          <OutboxList entries={failed} showActions />
        </div>
      )}
      {pending.length > 0 && (
        <div>
          <h3>Pending sync ({pending.length})</h3>
          <OutboxList entries={pending} showActions />
        </div>
      )}
      {synced.length > 0 && (
        <div>
          <h3>Synced ({synced.length})</h3>
          <OutboxList entries={[...synced].reverse()} />
        </div>
      )}

      <div className="card">
        <h3>How to test offline behaviour</h3>
        <ol>
          <li>Open the Mortality page and wait for the flock list to load.</li>
          <li>Stop the backend (press <kbd>Ctrl</kbd>+<kbd>C</kbd> in its terminal). Within about 10 seconds the header shows “Server unreachable”.</li>
          <li>Record deaths. The entry shows <em>Pending sync</em> and survives a page reload.</li>
          <li>Start the backend again. The entry becomes <em>Synced</em> by itself (or press “sync now”).</li>
        </ol>
        <p className="muted small">Limits: adding feed stock, creating farms/flocks and recording weights need the server. A reload while the server is down can only show lists that were saved on this device earlier.</p>
      </div>
    </section>
  );
}
