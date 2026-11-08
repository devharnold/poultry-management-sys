// One small wrapper around fetch(). Every API call goes through apiFetch so errors look the same everywhere.
const configured = import.meta.env?.VITE_API_BASE_URL;
export const API_BASE_URL = (configured || 'http://localhost:8080').replace(/\/+$/, '');

const TIMEOUT_MS = 10000;

export class ApiError extends Error {
  constructor(message, { status = 0, fieldErrors = {}, isNetworkError = false } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors || {};
    this.isNetworkError = isNetworkError;
  }

  // "Transient" = worth retrying later without changing anything: no connection, timeout, busy or crashed server.
  // A 400/404/409/422 answer is NOT transient: the server understood the request and refused it.
  get isTransient() {
    return this.isNetworkError || this.status === 408 || this.status === 429 || this.status >= 500;
  }
}

export async function apiFetch(path, { method = 'GET', body, fetchImpl, timeoutMs = TIMEOUT_MS } = {}) {
  const doFetch = fetchImpl || globalThis.fetch.bind(globalThis);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  let response;
  try {
    response = await doFetch(`${API_BASE_URL}${path}`, {
      method,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });
  } catch {
    throw new ApiError(
      'Cannot reach the server. Check that the backend is running and that you are online.',
      { isNetworkError: true },
    );
  } finally {
    clearTimeout(timer);
  }

  let data = null;
  const text = await response.text();
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    throw new ApiError(
      (data && (data.message || data.detail)) || `The server answered with an error (HTTP ${response.status}).`,
      { status: response.status, fieldErrors: data && data.fieldErrors },
    );
  }
  return data;
}
