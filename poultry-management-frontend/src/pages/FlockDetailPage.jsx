import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/endpoints.js';
import { useApi } from '../hooks/useApi.js';
import { useOutbox } from '../hooks/useOutbox.jsx';
import { useRole } from '../hooks/useRole.jsx';
import WeightForm from '../components/WeightForm.jsx';
import StatCard from '../components/StatCard.jsx';
import { Empty, ErrorNotice, Info, Loading } from '../components/Notice.jsx';
import { formatDate, formatDateTime, formatKg, formatNumber, formatPercent } from '../utils/format.js';

export default function FlockDetailPage() {
  const { id } = useParams();
  const { can } = useRole();
  const { dataVersion } = useOutbox();
  const [bump, setBump] = useState(0);
  const deps = [id, dataVersion, bump];

  const flockApi = useApi(() => api.flocks.get(id), deps);
  const perfApi = useApi(() => api.flocks.performance(id), deps);
  const mortalityApi = useApi(() => api.flocks.mortality(id), deps);
  const weightsApi = useApi(() => api.flocks.weights(id), deps);

  if (flockApi.loading && !flockApi.data) return <Loading />;
  if (flockApi.error) {
    return (
      <section>
        <ErrorNotice error={flockApi.error} onRetry={flockApi.reload} />
        <Link to="/flocks">Back to flocks</Link>
      </section>
    );
  }
  const flock = flockApi.data;
  const perf = perfApi.data;

  return (
    <section>
      <p><Link to="/flocks">← All flocks</Link></p>
      <h2>Flock {flock.flockCode}</h2>
      <p className="muted">
        {flock.farmName} · {flock.birdType} · placed {formatDate(flock.placementDate)} · {flock.status === 'ACTIVE' ? 'Active' : 'Completed'}
      </p>
      {flock.notes && <p>{flock.notes}</p>}

      <div className="stat-grid">
        <StatCard label="Initial birds" value={formatNumber(flock.initialBirdCount)} />
        <StatCard label="Total deaths recorded" value={formatNumber(flock.totalMortality)} />
        <StatCard label="Birds alive now" value={formatNumber(flock.currentBirdCount)} note="initial minus deaths" />
      </div>

      <div className="card">
        <h3>Performance indicators</h3>
        {perfApi.loading && !perf && <Loading />}
        <ErrorNotice error={perfApi.error} onRetry={perfApi.reload} />
        {perf && (
          <>
            <dl className="facts">
              <dt>Mortality rate</dt>
              <dd><strong>{formatPercent(perf.mortalityRatePercent)}</strong><br /><span className="muted small">{perf.mortalityFormula}</span></dd>
              <dt>Feed used by this flock</dt>
              <dd>{formatKg(perf.totalFeedUsedKg)} <span className="muted small">(only usage recorded against this flock)</span></dd>
              <dt>Latest average weight</dt>
              <dd>{perf.latestAverageWeightKg === null ? 'No weight recorded yet' : `${formatNumber(perf.latestAverageWeightKg)} kg per bird`}</dd>
              <dt>Feed conversion ratio (FCR)</dt>
              <dd>
                {perf.fcr === null ? (
                  <span className="badge badge-neutral">Not enough data yet</span>
                ) : (
                  <strong>{formatNumber(perf.fcr)}</strong>
                )}
                <br /><span className="small">{perf.fcrMessage}</span>
                <br /><span className="muted small">{perf.fcrFormula}</span>
              </dd>
            </dl>
            <p className="muted small">{perf.disclaimer}</p>
          </>
        )}
      </div>

      {can('recordWeights') && flock.status === 'ACTIVE' && <WeightForm flock={flock} onSaved={() => setBump((b) => b + 1)} />}
      {!can('recordWeights') && <Info>Your demo role cannot record weights.</Info>}

      <h3>Weight records</h3>
      <ErrorNotice error={weightsApi.error} onRetry={weightsApi.reload} />
      {weightsApi.data && weightsApi.data.length === 0 && <Empty title="No weights recorded yet" />}
      {weightsApi.data && weightsApi.data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Date</th><th>Average per bird</th><th>Birds weighed</th><th>Notes</th></tr></thead>
            <tbody>
              {weightsApi.data.map((w) => (
                <tr key={w.id}>
                  <td data-label="Date">{formatDate(w.recordDate)}</td>
                  <td data-label="Average per bird">{formatNumber(w.averageBirdWeightKg)} kg</td>
                  <td data-label="Birds weighed">{w.sampledBirdCount}</td>
                  <td data-label="Notes">{w.notes}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h3>Mortality history</h3>
      <ErrorNotice error={mortalityApi.error} onRetry={mortalityApi.reload} />
      {mortalityApi.data && mortalityApi.data.length === 0 && (
        <Empty title="No mortality recorded for this flock"><p><Link to="/mortality">Record mortality</Link></p></Empty>
      )}
      {mortalityApi.data && mortalityApi.data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Date</th><th>Deaths</th><th>Reason / notes</th><th>Saved on server</th></tr></thead>
            <tbody>
              {mortalityApi.data.map((m) => (
                <tr key={m.id}>
                  <td data-label="Date">{formatDate(m.recordDate)}</td>
                  <td data-label="Deaths"><strong>{m.count}</strong></td>
                  <td data-label="Reason / notes">{m.reason}</td>
                  <td data-label="Saved on server">{formatDateTime(m.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
