import { useCallback, useEffect, useState } from 'react';
import { readCache, writeCache } from '../utils/cache.js';

// Loads data when the component appears (and again when `deps` change or reload() is called).
// Returns { data, error, loading, reload, fromCache, cachedAt }.
// With { cacheKey }, a failed load caused by "server unreachable" falls back to the last good copy.
export function useApi(fetcher, deps = [], { cacheKey } = {}) {
  const [state, setState] = useState({ data: null, error: null, loading: true, fromCache: false, cachedAt: null });
  const [tick, setTick] = useState(0);
  const reload = useCallback(() => setTick((t) => t + 1), []);

  useEffect(() => {
    let cancelled = false;
    setState((s) => ({ ...s, loading: true, error: null }));
    fetcher()
      .then((data) => {
        if (cancelled) return;
        if (cacheKey) writeCache(cacheKey, data);
        setState({ data, error: null, loading: false, fromCache: false, cachedAt: null });
      })
      .catch((error) => {
        if (cancelled) return;
        const cached = cacheKey && error && error.isNetworkError ? readCache(cacheKey) : null;
        if (cached) {
          setState({ data: cached.data, error: null, loading: false, fromCache: true, cachedAt: cached.savedAt });
        } else {
          setState({ data: null, error, loading: false, fromCache: false, cachedAt: null });
        }
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);

  return { ...state, reload };
}
