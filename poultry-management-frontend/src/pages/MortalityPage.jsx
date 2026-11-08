import { useState } from 'react';
import { api } from '../api/endpoints.js';
import { useApi } from '../hooks/useApi.js';
import { useOutbox } from '../hooks/useOutbox.jsx';
import { useRole } from '../hooks/useRole.jsx';
import MortalityForm from '../components/MortalityForm.jsx';
import OutboxList from '../components/OutboxList.jsx';
import { Empty, ErrorNotice, Info, Loading } from '../components/Notice.jsx';
import { SelectField } from '../components/Field.jsx';
import { formatDate, formatDateTime } from '../utils/format.js';

export default function MortalityPage() {
  const { can } = useRole();
  const { dataVersion, entries } = useOutbox();
  const [filterFlock, setFilterFlock] = useState('');

  const flocksApi = useApi(() => api.flocks.list({ status: 'ACTIVE' }), [dataVersion], { cacheKey: 'flocks-active' });
  const historyApi = useApi(() => api.mortality.list({ flockId: filterFlock }), [filterFlock, dataVersion]);
  const flocks = flocksApi.data || [];

  // not-yet-confirmed entries first, then the 5 most recent confirmed ones
  const mine = entries.filter((e) => e.type === 'mortality');
  const unsynced = mine.filter((e) => e.status !== 'synced');
  const recentSynced = mine.filter((e) => e.status === 'synced').slice(-5);

  return (
    <section>
      <h2>Mortality</h2>

      {can('logMortality') ? (
        <>
          {flocksApi.loading && !flocksApi.data && <Loading text="Loading flocks…" />}
          {flocksApi.fromCache && (
            <Info tone="warning">
              The server cannot be reached, so this list of flocks is the copy saved on this device ({formatDateTime(flocksApi.cachedAt)}).
              Birds-alive numbers may be out of date. You can still record deaths: they will be sent later.
            </Info>
          )}
          {flocksApi.error && <ErrorNotice error={flocksApi.error} onRetry={flocksApi.reload} />}
          {flocksApi.data && flocks.length === 0 && (
            <Empty title="No active flocks">
              <p>Mortality is recorded against an active flock. Add a flock first (Flocks page), or load the demo data from the dashboard.</p>
            </Empty>
          )}
          {flocks.length > 0 && <MortalityForm flocks={flocks} />}
        </>
      ) : (
        <Info>Your demo role cannot record mortality.</Info>
      )}

      {(unsynced.length > 0 || recentSynced.length > 0) && (
        <div className="card">
          <h3>Entries made on this device</h3>
          <p className="muted small">“Pending sync” and “Sync failed” entries are not on the server yet. Manage them on the Sync page.</p>
          <OutboxList entries={[...unsynced, ...recentSynced]} />
        </div>
      )}

      <h3>Mortality history</h3>
      <div className="toolbar">
        <SelectField
          label="Show flock" value={filterFlock} onChange={(e) => setFilterFlock(e.target.value)} placeholder="All flocks"
          options={flocks.map((f) => ({ value: String(f.id), label: f.flockCode }))}
        />
      </div>
      {historyApi.loading && !historyApi.data && <Loading />}
      <ErrorNotice error={historyApi.error} onRetry={historyApi.reload} />
      {historyApi.data && historyApi.data.length === 0 && (
        <Empty title="No mortality recorded yet"><p>Records you save will appear here once the server has them.</p></Empty>
      )}
      {historyApi.data && historyApi.data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Date</th><th>Flock</th><th>Deaths</th><th>Reason / notes</th><th>Saved on server</th></tr></thead>
            <tbody>
              {historyApi.data.map((r) => (
                <tr key={r.id}>
                  <td data-label="Date">{formatDate(r.recordDate)}</td>
                  <td data-label="Flock">{r.flockCode}</td>
                  <td data-label="Deaths"><strong>{r.count}</strong></td>
                  <td data-label="Reason / notes">{r.reason}</td>
                  <td data-label="Saved on server">{formatDateTime(r.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
