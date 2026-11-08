import { useState } from 'react';
import { api } from '../api/endpoints.js';
import { useApi } from '../hooks/useApi.js';
import { useOutbox } from '../hooks/useOutbox.jsx';
import { useRole } from '../hooks/useRole.jsx';
import FeedItemForm from '../components/FeedItemForm.jsx';
import FeedUsageForm from '../components/FeedUsageForm.jsx';
import StockInForm from '../components/StockInForm.jsx';
import OutboxList from '../components/OutboxList.jsx';
import { Empty, ErrorNotice, Info, Loading } from '../components/Notice.jsx';
import { formatDate, formatDateTime, formatKg } from '../utils/format.js';

const MOVEMENT_LABEL = { STOCK_IN: 'Stock in', USAGE: 'Usage' };

export default function FeedPage() {
  const { can } = useRole();
  const { dataVersion, entries } = useOutbox();
  const [selectedId, setSelectedId] = useState(null);
  const [panel, setPanel] = useState(null); // 'usage' | 'stock' | 'history' | 'edit'
  const [creating, setCreating] = useState(false);
  const [saved, setSaved] = useState(null);
  const [bump, setBump] = useState(0);

  const itemsApi = useApi(() => api.feed.list(), [dataVersion], { cacheKey: 'feed-items' });
  const flocksApi = useApi(() => api.flocks.list({ status: 'ACTIVE' }), [dataVersion], { cacheKey: 'flocks-active' });
  const movementsApi = useApi(
    () => (selectedId && panel === 'history' ? api.feed.movements(selectedId) : Promise.resolve(null)),
    [selectedId, panel, dataVersion, bump],
  );

  const items = itemsApi.data || [];
  const flocks = flocksApi.data || [];
  const selected = items.find((i) => i.id === selectedId);

  function open(item, which) {
    setSaved(null);
    setCreating(false);
    setSelectedId(item.id);
    setPanel(which);
  }

  function refreshAfterChange() {
    itemsApi.reload();
    setBump((b) => b + 1);
  }

  const mine = entries.filter((e) => e.type === 'feed-usage');
  const unsynced = mine.filter((e) => e.status !== 'synced');

  return (
    <section>
      <h2>Feed inventory</h2>
      <p className="muted">All quantities are in kilograms (kg). Stock changes only through “Add stock” and “Record usage”, so the history always explains the number.</p>
      {saved && <Info tone="success">{saved}</Info>}

      {can('manageFeed') && !creating && (
        <button type="button" className="btn btn-primary" onClick={() => { setSaved(null); setPanel(null); setCreating(true); }}>Add a feed item</button>
      )}
      {creating && (
        <FeedItemForm
          onSaved={(item) => { setCreating(false); setSaved(`Feed item “${item.name}” created.`); itemsApi.reload(); }}
          onCancel={() => setCreating(false)}
        />
      )}

      {itemsApi.fromCache && (
        <Info tone="warning">
          The server cannot be reached: showing the feed list saved on this device ({formatDateTime(itemsApi.cachedAt)}). Quantities may be
          out of date. Feed usage you record is saved here and sent later.
        </Info>
      )}
      {itemsApi.loading && !itemsApi.data && <Loading />}
      {itemsApi.error && <ErrorNotice error={itemsApi.error} onRetry={itemsApi.reload} />}
      {itemsApi.data && items.length === 0 && (
        <Empty title="No feed items yet"><p>Add a feed item (with an opening stock) to start tracking stock.</p></Empty>
      )}

      {items.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Feed</th><th>Type</th><th>In stock</th><th>Reorder at</th><th>Status</th><th><span className="sr-only">Actions</span></th></tr></thead>
            <tbody>
              {items.map((i) => (
                <tr key={i.id} className={i.id === selectedId ? 'row-selected' : undefined}>
                  <td data-label="Feed"><strong>{i.name}</strong></td>
                  <td data-label="Type">{i.feedType}</td>
                  <td data-label="In stock"><strong>{formatKg(i.quantityOnHand)}</strong></td>
                  <td data-label="Reorder at">{formatKg(i.reorderThreshold)}</td>
                  <td data-label="Status">
                    {i.lowStock ? <span className="badge badge-bad">Low stock</span> : <span className="badge badge-ok">OK</span>}
                  </td>
                  <td>
                    <div className="btn-row">
                      {can('recordFeedUsage') && <button type="button" className="btn btn-small" onClick={() => open(i, 'usage')}>Record usage</button>}
                      {can('addStock') && <button type="button" className="btn btn-small" onClick={() => open(i, 'stock')}>Add stock</button>}
                      <button type="button" className="btn btn-small" onClick={() => open(i, 'history')}>History</button>
                      {can('manageFeed') && <button type="button" className="btn btn-small" onClick={() => open(i, 'edit')}>Edit</button>}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selected && panel === 'usage' && <FeedUsageForm key={`u${selected.id}`} item={selected} flocks={flocks} />}
      {selected && panel === 'stock' && <StockInForm key={`s${selected.id}`} item={selected} onSaved={refreshAfterChange} />}
      {selected && panel === 'edit' && (
        <FeedItemForm
          key={`e${selected.id}`} item={selected}
          onSaved={(item) => { setPanel(null); setSaved(`Feed item “${item.name}” saved.`); itemsApi.reload(); }}
          onCancel={() => setPanel(null)}
        />
      )}
      {selected && panel === 'history' && (
        <div className="card">
          <h3>Movement history: {selected.name}</h3>
          {movementsApi.loading && !movementsApi.data && <Loading />}
          <ErrorNotice error={movementsApi.error} onRetry={movementsApi.reload} />
          {movementsApi.data && movementsApi.data.length === 0 && <p className="muted">No movements yet.</p>}
          {movementsApi.data && movementsApi.data.length > 0 && (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Date</th><th>Type</th><th>Quantity</th><th>Stock after</th><th>Flock</th><th>Notes</th></tr></thead>
                <tbody>
                  {movementsApi.data.map((m) => (
                    <tr key={m.id}>
                      <td data-label="Date">{formatDate(m.movementDate)}</td>
                      <td data-label="Type">{MOVEMENT_LABEL[m.movementType]}</td>
                      <td data-label="Quantity">{m.movementType === 'USAGE' ? '−' : '+'}{formatKg(m.quantity)}</td>
                      <td data-label="Stock after">{formatKg(m.quantityAfter)}</td>
                      <td data-label="Flock">{m.flockCode}</td>
                      <td data-label="Notes">{m.notes}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {unsynced.length > 0 && (
        <div className="card">
          <h3>Feed usage not yet confirmed by the server</h3>
          <OutboxList entries={unsynced} />
        </div>
      )}
    </section>
  );
}
