// The offline queue ("outbox").
//
// Flow for every mortality / feed-usage submit:
//   1. save the entry on this device with status "pending"   (never lost, even if the tab closes)
//   2. try to send it to the server
//        success                       -> "synced"
//        no connection / timeout / 5xx -> stays "pending", retried later (same id, so no duplicates)
//        server says "no" (400/404/409/422) -> "failed" with the reason, shown to the user, never dropped
//
// This file has no React or browser code in it, so it is tested with plain Node (outbox.test.js).
export const STATUS = { PENDING: 'pending', SYNCED: 'synced', FAILED: 'failed' };

const MAX_SYNCED_KEPT = 50; // only already-confirmed entries are ever pruned automatically

const byCreated = (a, b) =>
  a.createdAt < b.createdAt ? -1 : a.createdAt > b.createdAt ? 1 : a.id < b.id ? -1 : 1;

export function createOutbox({ storage, send, newId, now = () => new Date().toISOString() }) {
  const listeners = new Set();
  let running = null;

  const notify = () => listeners.forEach((fn) => fn());

  async function list() {
    const all = await storage.getAll();
    return all.sort(byCreated);
  }

  async function add({ type, payload, label }) {
    const entry = {
      id: newId(),
      type,
      payload,
      label,
      status: STATUS.PENDING,
      attempts: 0,
      lastError: null,
      fieldErrors: {},
      createdAt: now(),
      lastAttemptAt: null,
      syncedAt: null,
    };
    await storage.put(entry);
    await pruneSynced();
    notify();
    return entry;
  }

  async function pruneSynced() {
    const synced = (await list()).filter((e) => e.status === STATUS.SYNCED);
    const extra = synced.length - MAX_SYNCED_KEPT;
    for (let i = 0; i < extra; i += 1) await storage.delete(synced[i].id);
  }

  // Writes the new state only if the entry still exists (it may have been discarded while sending).
  async function save(entry) {
    if (await storage.get(entry.id)) await storage.put(entry);
  }

  async function sendOne(entry) {
    try {
      await send(entry);
      await save({
        ...entry,
        status: STATUS.SYNCED,
        attempts: entry.attempts + 1,
        lastError: null,
        fieldErrors: {},
        lastAttemptAt: now(),
        syncedAt: now(),
      });
      return STATUS.SYNCED;
    } catch (error) {
      const transient = Boolean(error && error.isTransient);
      await save({
        ...entry,
        status: transient ? STATUS.PENDING : STATUS.FAILED,
        attempts: entry.attempts + 1,
        lastError: (error && error.message) || 'Unknown error',
        fieldErrors: (error && error.fieldErrors) || {},
        lastAttemptAt: now(),
      });
      return transient ? STATUS.PENDING : STATUS.FAILED;
    }
  }

  // Sends every pending entry, oldest first. Stops at the first connection problem
  // (no point hammering a server we cannot reach). Never runs twice at once.
  function syncAll() {
    if (running) return running.then(() => syncAll());
    running = (async () => {
      const result = { synced: 0, failed: 0, pending: 0 };
      const pending = (await list()).filter((e) => e.status === STATUS.PENDING);
      for (const entry of pending) {
        const outcome = await sendOne(entry);
        notify();
        if (outcome === STATUS.SYNCED) result.synced += 1;
        else if (outcome === STATUS.FAILED) result.failed += 1;
        else break;
      }
      result.pending = (await list()).filter((e) => e.status === STATUS.PENDING).length;
      return result;
    })().finally(() => {
      running = null;
      notify();
    });
    return running;
  }

  async function retry(id) {
    const entry = await storage.get(id);
    if (entry && entry.status === STATUS.FAILED) {
      await storage.put({ ...entry, status: STATUS.PENDING });
      notify();
    }
    return syncAll();
  }

  async function discard(id) {
    await storage.delete(id);
    notify();
  }

  async function clearSynced() {
    for (const entry of await list()) {
      if (entry.status === STATUS.SYNCED) await storage.delete(entry.id);
    }
    notify();
  }

  function subscribe(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  }

  return { list, add, syncAll, retry, discard, clearSynced, subscribe, get: (id) => storage.get(id) };
}
