import { NavLink, Outlet } from 'react-router-dom';
import SyncBadge from './SyncBadge.jsx';
import RoleSwitcher from './RoleSwitcher.jsx';
import { useOutbox } from '../hooks/useOutbox.jsx';

const LINKS = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/farms', label: 'Farms' },
  { to: '/flocks', label: 'Flocks' },
  { to: '/mortality', label: 'Mortality' },
  { to: '/feed', label: 'Feed' },
  { to: '/alerts', label: 'Alerts' },
  { to: '/sync', label: 'Sync' },
];

export default function Layout() {
  const { pendingCount, failedCount } = useOutbox();
  const waiting = pendingCount + failedCount;
  return (
    <>
      <a className="skip-link" href="#main">Skip to content</a>
      <header className="app-header">
        <div className="app-header-inner">
          <h1 className="app-title">Poultry Management System <span className="tag">prototype</span></h1>
          <p className="app-sub">Case study: Kenchic · educational prototype, not an official or production system</p>
          <SyncBadge />
          <RoleSwitcher />
        </div>
        <nav aria-label="Main" className="nav">
          <ul>
            {LINKS.map((l) => (
              <li key={l.to}>
                <NavLink to={l.to} end={l.end} className={({ isActive }) => (isActive ? 'active' : undefined)}>
                  {l.label}
                  {l.to === '/sync' && waiting > 0 && <span className="pill pill-warn">{waiting}</span>}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      </header>
      <main id="main" className="app-main" tabIndex={-1}>
        <Outlet />
      </main>
      <footer className="app-footer">
        <p>
          Educational prototype. Sample data named “DEMO” is synthetic. Alerts, thresholds and indicators are unvalidated
          placeholders and must be reviewed by qualified poultry professionals before any real use. No login: do not enter confidential data.
        </p>
      </footer>
    </>
  );
}
