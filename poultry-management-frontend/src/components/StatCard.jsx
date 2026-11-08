export default function StatCard({ label, value, note, tone }) {
  return (
    <div className={`stat${tone ? ` stat-${tone}` : ''}`}>
      <p className="stat-label">{label}</p>
      <p className="stat-value">{value}</p>
      {note && <p className="stat-note">{note}</p>}
    </div>
  );
}
