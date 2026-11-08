import { useState } from 'react';
import { api } from '../api/endpoints.js';
import { useApi } from '../hooks/useApi.js';
import { useOutbox } from '../hooks/useOutbox.jsx';
import { useRole } from '../hooks/useRole.jsx';
import AlertItem from '../components/AlertItem.jsx';
import { SelectField } from '../components/Field.jsx';
import { Empty, ErrorNotice, Info, Loading } from '../components/Notice.jsx';

export default function AlertsPage() {
  const { can } = useRole();
  const { dataVersion } = useOutbox();
  const [status, setStatus] = useState('OPEN');
  const [actionError, setActionError] = useState(null);
  const { data, error, loading, reload } = useApi(() => api.alerts.list(status), [status, dataVersion]);

  async function resolve(alert) {
    setActionError(null);
    try {
      await api.alerts.resolve(alert.id);
      reload();
    } catch (err) {
      setActionError(err);
    }
  }

  return (
    <section>
      <h2>Alerts</h2>
      <Info>
        These alerts are created by this prototype's own rules (high daily mortality, low feed stock) and are only shown here. No SMS or email
        is sent. The thresholds are placeholders for demonstration and must be set by qualified poultry professionals.
      </Info>
      <div className="toolbar">
        <SelectField
          label="Show" value={status} onChange={(e) => setStatus(e.target.value)} placeholder="All alerts"
          options={[{ value: 'OPEN', label: 'Open' }, { value: 'RESOLVED', label: 'Resolved' }]}
        />
      </div>
      <ErrorNotice error={actionError} />
      {loading && !data && <Loading />}
      <ErrorNotice error={error} onRetry={reload} />
      {data && data.length === 0 && (
        <Empty title={status === 'OPEN' ? 'No open alerts' : 'No alerts to show'}>
          <p>Alerts appear when daily mortality reaches the threshold or feed stock falls to its reorder level.</p>
        </Empty>
      )}
      {data && data.length > 0 && (
        <ul className="alert-list">
          {data.map((a) => <AlertItem key={a.id} alert={a} onResolve={can('resolveAlerts') ? resolve : undefined} />)}
        </ul>
      )}
      {data && data.some((a) => a.status === 'OPEN') && !can('resolveAlerts') && (
        <p className="muted small">Your demo role cannot resolve alerts.</p>
      )}
    </section>
  );
}
