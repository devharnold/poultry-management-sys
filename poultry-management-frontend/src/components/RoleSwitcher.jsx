import { useId } from 'react';
import { useRole } from '../hooks/useRole.jsx';
import { ROLES } from '../utils/roles.js';

export default function RoleSwitcher() {
  const { role, setRole } = useRole();
  const id = useId();
  return (
    <div className="role-switcher">
      <label htmlFor={id}>Demo role</label>
      <select id={id} value={role} onChange={(e) => setRole(e.target.value)}>
        {ROLES.map((r) => (
          <option key={r.id} value={r.id}>{r.label}</option>
        ))}
      </select>
      <span className="muted small">Changes what this screen shows. Not a login: nothing is protected.</span>
    </div>
  );
}
