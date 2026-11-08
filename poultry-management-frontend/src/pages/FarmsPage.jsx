import { useState } from 'react';
import { api } from '../api/endpoints.js';
import { useApi } from '../hooks/useApi.js';
import { useRole } from '../hooks/useRole.jsx';
import FarmForm from '../components/FarmForm.jsx';
import { Empty, ErrorNotice, Info, Loading } from '../components/Notice.jsx';
import { TextField } from '../components/Field.jsx';

export default function FarmsPage() {
  const { can } = useRole();
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState(null); // null = closed, 'new' = add form, farm object = edit form
  const [saved, setSaved] = useState(null);
  const { data, error, loading, reload } = useApi(() => api.farms.list(search), [search]);

  function handleSaved(farm) {
    setEditing(null);
    setSaved(`Farm “${farm.name}” saved.`);
    reload();
  }

  return (
    <section>
      <h2>Farms</h2>
      {saved && <Info tone="success">{saved}</Info>}
      {can('manageFarms') ? (
        !editing && <button type="button" className="btn btn-primary" onClick={() => { setSaved(null); setEditing('new'); }}>Add a farm</button>
      ) : (
        <Info>Your demo role can view farms but not change them. Switch the demo role at the top to try the other screens.</Info>
      )}
      {editing && (
        <FarmForm
          key={editing === 'new' ? 'new' : editing.id}
          farm={editing === 'new' ? null : editing}
          onSaved={handleSaved}
          onCancel={() => setEditing(null)}
        />
      )}

      <div className="toolbar">
        <TextField label="Search farms" type="search" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Name or location" />
      </div>

      {loading && !data && <Loading />}
      <ErrorNotice error={error} onRetry={reload} />
      {data && data.length === 0 && (
        <Empty title={search ? 'No farms match your search' : 'No farms yet'}>
          {!search && <p>Add a farm first; flocks belong to a farm.</p>}
        </Empty>
      )}
      {data && data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Name</th><th>Location</th><th>Flocks</th><th>Contact / notes</th>{can('manageFarms') && <th><span className="sr-only">Actions</span></th>}</tr>
            </thead>
            <tbody>
              {data.map((farm) => (
                <tr key={farm.id}>
                  <td data-label="Name"><strong>{farm.name}</strong></td>
                  <td data-label="Location">{farm.location}</td>
                  <td data-label="Flocks">{farm.flockCount}</td>
                  <td data-label="Contact / notes">{farm.contactNotes}</td>
                  {can('manageFarms') && (
                    <td><button type="button" className="btn btn-small" onClick={() => { setSaved(null); setEditing(farm); }}>Edit</button></td>
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
