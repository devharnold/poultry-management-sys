// A tiny "last known good answer" cache in localStorage. It lets the mortality and feed-usage forms
// still offer the flock / feed lists when the backend cannot be reached. The copy may be out of date.
const PREFIX = 'poultry.cache.';

export function writeCache(key, data) {
  try {
    localStorage.setItem(PREFIX + key, JSON.stringify({ savedAt: new Date().toISOString(), data }));
  } catch {
    // storage full or blocked: caching is optional, so ignore
  }
}

export function readCache(key) {
  try {
    const raw = localStorage.getItem(PREFIX + key);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}
