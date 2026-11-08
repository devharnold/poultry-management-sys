import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/endpoints.js';
import { useApi } from '../hooks/useApi.js';
import { useOutbox } from '../hooks/useOutbox.jsx';
import { useRole } from '../hooks/useRole.jsx';
import FlockForm from '../components/FlockForm.jsx';
import { Empty, ErrorNotice, Info, Loading } from '../components/Notice.jsx';
import { SelectField, TextField } from '../components/Field.jsx';
import { formatDate } from '../utils/format.js';

export default function FlocksPage() {
  const { can } = useRole();
  const { dataVersion } = useOutbox();
  const [filters, setFilters] = useState({ q: '', status: '', farmId: '' });
  const [editing, setEditing] = useState(null); // null | 'new' | flock
  const [saved, setSaved] = useState(null);

  const farmsApi = useApi(() => api.farms.list(), []);
  const flocksApi = useApi(() => api.flocks.list(filters), [filters, dataVersion]);
  const farms = farmsApi.data || [];
  const set = (field) => (e) => setFilters((f) => ({ ...f, [field]: e.target.value }));

  function handleSaved(flock) {
    setEditing(null);
    setSaved(`Flock ${flock.flockCode} saved.`);
    flocksApi.reload();
  }

  const { data, error, loading, reload } = flocksApi;

  return (
    <section>
      <h2>Flocks</h2>
      {saved && <Info tone="success">{saved}</Info>}
      {can('manageFlocks') ? (
        !editing && (
          <button type="button" className="btn btn-primary" onClick={() => { setSaved(null); setEditing('new'); }} disabled={farms.length === 0}>
            Add a flock
          </button>
        )
      ) : (
        <Info>Your demo role can view flocks but not create or edit them.</Info>
      )}
      {can('manageFlocks') && farmsApi.data && farms.length === 0 && (
        <Info tone="warning">Add a farm first (see <Link to="/farms">Farms</Link>); every flock belongs to a farm.</Info>
      )}
      {editing && (
        <FlockForm
          key={editing === 'new' ? 'new' : editing.id}
          flock={editing === 'new' ? null : editing}
          farms={farms}
          onSaved={handleSaved}
          onCancel={() => setEditing(null)}
        />
      )}

      <div className="toolbar form-grid">
        <TextField label="Search" type="search" value={filters.q} onChange={set('q')} placeholder="Code, bird type or farm" />
        <SelectField label="Status" value={filters.status} onChange={set('status')} placeholder="All" options={[{ value: 'ACTIVE', label: 'Active' }, { value: 'COMPLETED', label: 'Completed' }]} />
        <SelectField label="Farm" value={filters.farmId} onChange={set('farmId')} placeholder="All farms" options={farms.map((f) => ({ value: String(f.id), label: f.name }))} />
      </div>

      {loading && !data && <Loading />}
      <ErrorNotice error={error} onRetry={reload} />
      {data && data.length === 0 && (
        <Empty title="No flocks to show">
          <p>{filters.q || filters.status || filters.farmId ? 'No flock matches these filters.' : 'No flocks yet. Add one to start recording mortality and feed.'}</p>
        </Empty>
      )}
      {data && data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Code</th><th>Farm</th><th>Bird type</th><th>Placed</th><th>Initial</th><th>Deaths</th><th>Alive now</th><th>Status</th>{can('manageFlocks') && <th><span className="sr-only">Actions</span></th>}</tr>
            </thead>
            <tbody>
              {data.map((f) => (
                <tr key={f.id}>
                  <td data-label="Code"><Link to={`/flocks/${f.id}`}><strong>{f.flockCode}</strong></Link></td>
                  <td data-label="Farm">{f.farmName}</td>
                  <td data-label="Bird type">{f.birdType}</td>
                  <td data-label="Placed">{formatDate(f.placementDate)}</td>
                  <td data-label="Initial">{f.initialBirdCount}</td>
                  <td data-label="Deaths">{f.totalMortality}</td>
                  <td data-label="Alive now"><strong>{f.currentBirdCount}</strong></td>
                  <td data-label="Status"><span className={`badge badge-${f.status === 'ACTIVE' ? 'ok' : 'neutral'}`}>{f.status === 'ACTIVE' ? 'Active' : 'Completed'}</span></td>
                  {can('manageFlocks') && (
                    <td><button type="button" className="btn btn-small" onClick={() => { setSaved(null); setEditing(f); }}>Edit</button></td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
