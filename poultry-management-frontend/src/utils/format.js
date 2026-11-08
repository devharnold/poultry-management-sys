export function formatDate(iso) {
  if (!iso) return '';
  const [y, m, d] = String(iso).slice(0, 10).split('-');
  return y && m && d ? `${d}/${m}/${y}` : String(iso);
}

export function formatDateTime(iso) {
  if (!iso) return '';
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? String(iso) : date.toLocaleString();
}

export function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '';
  return Number(value).toLocaleString(undefined, { maximumFractionDigits: 3 });
}

// Every feed quantity in this prototype is in kilograms.
export function formatKg(value) {
  const text = formatNumber(value);
  return text === '' ? '' : `${text} kg`;
}

export function formatPercent(value) {
  if (value === null || value === undefined) return '';
  return `${Number(value).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} %`;
}

// Today as YYYY-MM-DD in the browser's own time zone.
export function todayIso(now = new Date()) {
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export const OUTBOX_STATUS_LABEL = {
  pending: 'Pending sync',
  synced: 'Synced',
  failed: 'Sync failed',
};

export const SEVERITY_LABEL = { INFO: 'Info', WARNING: 'Warning', CRITICAL: 'Critical' };
export const ALERT_TYPE_LABEL = { HIGH_MORTALITY: 'High mortality', LOW_STOCK: 'Low feed stock' };
