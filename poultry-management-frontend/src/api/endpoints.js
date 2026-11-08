// Every backend URL the app uses, in one file. Components never build URLs themselves.
import { apiFetch } from './client.js';

function qs(params = {}) {
  const parts = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && String(v).trim() !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`);
  return parts.length ? `?${parts.join('&')}` : '';
}

const post = (path, body) => apiFetch(path, { method: 'POST', body });
const put = (path, body) => apiFetch(path, { method: 'PUT', body });

export const api = {
  health: () => apiFetch('/api/health', { timeoutMs: 4000 }),
  dashboard: () => apiFetch('/api/dashboard'),
  seedDemo: () => post('/api/demo/seed'),

  farms: {
    list: (q) => apiFetch(`/api/farms${qs({ q })}`),
    create: (body) => post('/api/farms', body),
    update: (id, body) => put(`/api/farms/${id}`, body),
  },

  flocks: {
    list: (filters) => apiFetch(`/api/flocks${qs(filters)}`),
    get: (id) => apiFetch(`/api/flocks/${id}`),
    create: (body) => post('/api/flocks', body),
    update: (id, body) => put(`/api/flocks/${id}`, body),
    mortality: (id) => apiFetch(`/api/flocks/${id}/mortality`),
    weights: (id) => apiFetch(`/api/flocks/${id}/weights`),
    addWeight: (id, body) => post(`/api/flocks/${id}/weights`, body),
    performance: (id) => apiFetch(`/api/flocks/${id}/performance`),
  },

  mortality: {
    list: (filters) => apiFetch(`/api/mortality${qs(filters)}`),
    create: (body) => post('/api/mortality', body),
  },

  feed: {
    list: () => apiFetch('/api/feed-items'),
    create: (body) => post('/api/feed-items', body),
    update: (id, body) => put(`/api/feed-items/${id}`, body),
    stockIn: (id, body) => post(`/api/feed-items/${id}/stock-in`, body),
    usage: (id, body) => post(`/api/feed-items/${id}/usage`, body),
    movements: (id) => apiFetch(`/api/feed-items/${id}/movements`),
  },

  alerts: {
    list: (status) => apiFetch(`/api/alerts${qs({ status })}`),
    resolve: (id) => post(`/api/alerts/${id}/resolve`),
  },
};

// How each kind of offline entry is sent to the server. The entry id doubles as the clientOperationId,
// so re-sending the same entry can never create a second record on the server.
export function sendOutboxEntry(entry) {
  if (entry.type === 'mortality') {
    return api.mortality.create({ ...entry.payload, clientOperationId: entry.id });
  }
  if (entry.type === 'feed-usage') {
    const { feedItemId, ...rest } = entry.payload;
    return api.feed.usage(feedItemId, { ...rest, clientOperationId: entry.id });
  }
  return Promise.reject(new Error(`Unknown offline entry type: ${entry.type}`));
}
