// Where offline entries are kept on this device. Same tiny interface for every option:
//   getAll() / get(id) / put(entry) / delete(id)      (all async)
// Preferred: IndexedDB. Fallbacks: localStorage, then memory (memory is lost when the tab closes).
const DB_NAME = 'poultry-outbox';
const STORE = 'entries';
const clone = (value) => JSON.parse(JSON.stringify(value));

export function createMemoryStorage() {
  const map = new Map();
  return {
    kind: 'memory',
    persistent: false,
    async getAll() { return Array.from(map.values(), clone); },
    async get(id) { return map.has(id) ? clone(map.get(id)) : undefined; },
    async put(entry) { map.set(entry.id, clone(entry)); },
    async delete(id) { map.delete(id); },
  };
}

export function createLocalStorageStorage(key = 'poultry-outbox-v1') {
  const read = () => {
    try { return JSON.parse(localStorage.getItem(key) || '{}'); } catch { return {}; }
  };
  const write = (obj) => localStorage.setItem(key, JSON.stringify(obj));
  return {
    kind: 'localStorage',
    persistent: true,
    async getAll() { return Object.values(read()); },
    async get(id) { return read()[id]; },
    async put(entry) { const all = read(); all[entry.id] = entry; write(all); },
    async delete(id) { const all = read(); delete all[id]; write(all); },
  };
}

function openDatabase(name) {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(name, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(STORE, { keyPath: 'id' });
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
    request.onblocked = () => reject(new Error('IndexedDB is blocked'));
  });
}

export function createIndexedDbStorage(name = DB_NAME) {
  let dbPromise = null;
  const db = () => {
    if (!dbPromise) dbPromise = openDatabase(name);
    return dbPromise;
  };
  const run = async (mode, action) => {
    const database = await db();
    return new Promise((resolve, reject) => {
      const tx = database.transaction(STORE, mode);
      const request = action(tx.objectStore(STORE));
      tx.oncomplete = () => resolve(request ? request.result : undefined);
      tx.onerror = () => reject(tx.error);
      tx.onabort = () => reject(tx.error);
    });
  };
  return {
    kind: 'IndexedDB',
    persistent: true,
    getAll: () => run('readonly', (s) => s.getAll()),
    get: (id) => run('readonly', (s) => s.get(id)),
    put: (entry) => run('readwrite', (s) => s.put(entry)),
    delete: (id) => run('readwrite', (s) => s.delete(id)),
    probe: () => db(),
  };
}

// Picks the best storage that actually works in this browser.
// The returned object's `kind` is updated so the Sync page can tell the user where entries live.
export function createDefaultStorage() {
  let backend = null;
  const result = { kind: 'checking…', persistent: false };

  async function pick() {
    if (backend) return backend;
    if (typeof indexedDB !== 'undefined') {
      try {
        const idb = createIndexedDbStorage();
        await idb.probe();
        backend = idb;
      } catch {
        backend = null;
      }
    }
    if (!backend && typeof localStorage !== 'undefined') {
      try {
        localStorage.setItem('poultry-outbox-test', '1');
        localStorage.removeItem('poultry-outbox-test');
        backend = createLocalStorageStorage();
      } catch {
        backend = null;
      }
    }
    if (!backend) backend = createMemoryStorage();
    result.kind = backend.kind;
    result.persistent = backend.persistent;
    return backend;
  }

  for (const method of ['getAll', 'get', 'put', 'delete']) {
    result[method] = async (...args) => (await pick())[method](...args);
  }
  result.init = pick;
  return result;
}
