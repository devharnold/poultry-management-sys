import { ALERT_TYPE_LABEL, SEVERITY_LABEL, formatDateTime } from '../utils/format.js';

// Severity is shown with TEXT as well as colour, so it is readable without seeing colours.
export default function AlertItem({ alert, onResolve }) {
  return (
    <li className={`alert alert-${alert.severity.toLowerCase()}${alert.status === 'RESOLVED' ? ' alert-resolved' : ''}`}>
      <div className="alert-head">
        <span className="badge badge-sev">{SEVERITY_LABEL[alert.severity] || alert.severity}</span>
        <span className="badge">{ALERT_TYPE_LABEL[alert.alertType] || alert.alertType}</span>
        <span className="badge">{alert.status === 'OPEN' ? 'Open' : 'Resolved'}</span>
        <span className="badge badge-proto" title="Created by this prototype's own rules. No SMS or email is sent.">
          Prototype-generated
        </span>
      </div>
      <p className="alert-message">{alert.message}</p>
      <p className="muted small">
        Raised {formatDateTime(alert.createdAt)}
        {alert.resolvedAt ? ` · resolved ${formatDateTime(alert.resolvedAt)}` : ''}
      </p>
      {onResolve && alert.status === 'OPEN' && (
        <button type="button" className="btn btn-small" onClick={() => onResolve(alert)}>Mark as resolved</button>
      )}
    </li>
  );
}
