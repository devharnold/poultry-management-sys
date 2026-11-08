import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/endpoints.js';
import { useApi } from '../hooks/useApi.js';
import { useOutbox } from '../hooks/useOutbox.jsx';
import StatCard from '../components/StatCard.jsx';
import AlertItem from '../components/AlertItem.jsx';
import { Empty, ErrorNotice, Info, Loading } from '../components/Notice.jsx';
import { formatDate, formatKg, formatNumber } from '../utils/format.js';

export default function DashboardPage() {
  const { dataVersion } = useOutbox();
  const { data, error, loading, reload } = useApi(() => api.dashboard(), [dataVersion]);
  const [seeding, setSeeding] = useState(false);
  const [seedError, setSeedError] = useState(null);

  async function loadDemo() {
    setSeeding(true);
    setSeedError(null);
    try {
      await api.seedDemo();
      reload();
    } catch (err) {
      setSeedError(err);
    } finally {
      setSeeding(false);
    }
  }

  if (loading && !data) return <Loading text="Loading dashboard…" />;
  if (error) return <ErrorNotice error={error} onRetry={reload} />;

  const isEmpty = data.farmCount === 0 && data.totalActiveFlocks === 0;

  return (
    <section>
      <h2>Dashboard</h2>
      <p className="muted">
        {data.dataNote} Today is {formatDate(data.today)} ({data.timezone}).
      </p>
      {data.demoDataLoaded && (
        <Info tone="warning">
          <strong>Demo data is loaded.</strong> Records named “DEMO” are synthetic examples, not real farm data.
        </Info>
      )}

      {isEmpty && (
        <Empty title="No data yet">
          <p>The database is empty. Add your first farm and flock, or load clearly-labelled synthetic demo data to explore the screens.</p>
          <div className="btn-row">
            <Link className="btn btn-primary" to="/farms">Add a farm</Link>
            <button type="button" className="btn" onClick={loadDemo} disabled={seeding}>
              {seeding ? 'Loading…' : 'Load synthetic demo data'}
            </button>
          </div>
          {seedError && <ErrorNotice error={seedError} />}
        </Empty>
      )}

      <div className="stat-grid">
        <StatCard label="Active flocks" value={formatNumber(data.totalActiveFlocks)} />
        <StatCard label="Birds alive (active flocks)" value={formatNumber(data.totalBirds)} note="initial birds minus recorded deaths" />
        <StatCard label="Deaths recorded today" value={formatNumber(data.mortalityToday)} />
        <StatCard
          label="Feed items at or below reorder level"
          value={formatNumber(data.lowStockCount)}
          tone={data.lowStockCount > 0 ? 'warn' : undefined}
        />
        <StatCard label="Open alerts" value={formatNumber(data.openAlertCount)} tone={data.openAlertCount > 0 ? 'warn' : undefined} />
      </div>

      <div className="two-col">
        <div className="card">
          <h3>Low feed stock</h3>
          {data.lowStockItems.length === 0 ? (
            <p className="muted">No feed item is at or below its reorder threshold.</p>
          ) : (
            <ul className="plain-list">
              {data.lowStockItems.map((item) => (
                <li key={item.id}>
                  <strong>{item.name}</strong>: {formatKg(item.quantityOnHand)} on hand (reorder at {formatKg(item.reorderThreshold)})
                </li>
              ))}
            </ul>
          )}
          <Link to="/feed">Go to feed inventory</Link>
        </div>

        <div className="card">
          <h3>Recent alerts</h3>
          {data.recentAlerts.length === 0 ? (
            <p className="muted">No alerts have been raised.</p>
          ) : (
            <ul className="alert-list">
              {data.recentAlerts.map((a) => <AlertItem key={a.id} alert={a} />)}
            </ul>
          )}
          <Link to="/alerts">See all alerts</Link>
        </div>
      </div>
    </section>
  );
}
