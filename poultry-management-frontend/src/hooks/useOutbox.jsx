import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { createOutbox, STATUS } from '../api/outbox.js';
import { createDefaultStorage } from '../api/outboxStorage.js';
import { sendOutboxEntry } from '../api/endpoints.js';
import { newId } from '../utils/uuid.js';
import { useConnectivity } from './useConnectivity.jsx';

const OutboxContext = createContext(null);
const RETRY_EVERY_MS = 30000;

// Gives every page access to the offline queue and tells them when data on the server has changed
// (`dataVersion` goes up after entries were synced, so pages reload their lists).
export function OutboxProvider({ children }) {
  const holder = useRef(null);
  if (!holder.current) {
    const storage = createDefaultStorage();
    holder.current = { storage, outbox: createOutbox({ storage, send: sendOutboxEntry, newId }) };
  }
  const { storage, outbox } = holder.current;

  const { apiReachable } = useConnectivity();
  const [entries, setEntries] = useState([]);
  const [dataVersion, setDataVersion] = useState(0);
  const [storageKind, setStorageKind] = useState(storage.kind);
  const [syncing, setSyncing] = useState(false);
  const entriesRef = useRef(entries);
  entriesRef.current = entries;

  const refresh = useCallback(async () => {
    setEntries(await outbox.list());
    setStorageKind(storage.kind);
  }, [outbox, storage]);

  useEffect(() => {
    storage.init().then(refresh);
    return outbox.subscribe(refresh);
  }, [outbox, storage, refresh]);

  const syncNow = useCallback(async () => {
    setSyncing(true);
    try {
      const result = await outbox.syncAll();
      if (result.synced > 0) setDataVersion((v) => v + 1);
      return result;
    } finally {
      setSyncing(false);
    }
  }, [outbox]);

  // send automatically when the backend becomes reachable, and every 30 s while something is waiting
  useEffect(() => {
    if (apiReachable === true) syncNow();
  }, [apiReachable, syncNow]);

  useEffect(() => {
    const timer = setInterval(() => {
      if (entriesRef.current.some((e) => e.status === STATUS.PENDING)) syncNow();
    }, RETRY_EVERY_MS);
    return () => clearInterval(timer);
  }, [syncNow]);

  // Saves on this device FIRST, then tries to send. Resolves with the entry's final state.
  const enqueue = useCallback(
    async (type, payload, label) => {
      const entry = await outbox.add({ type, payload, label });
      await syncNow();
      return (await outbox.get(entry.id)) || entry;
    },
    [outbox, syncNow],
  );

  const retry = useCallback(
    async (id) => {
      const result = await outbox.retry(id);
      if (result.synced > 0) setDataVersion((v) => v + 1);
    },
    [outbox],
  );

  const value = useMemo(
    () => ({
      entries,
      dataVersion,
      syncing,
      storageKind,
      persistent: storage.persistent,
      pendingCount: entries.filter((e) => e.status === STATUS.PENDING).length,
      failedCount: entries.filter((e) => e.status === STATUS.FAILED).length,
      enqueue,
      syncNow,
      retry,
      discard: outbox.discard,
      clearSynced: outbox.clearSynced,
    }),
    [entries, dataVersion, syncing, storageKind, storage, enqueue, syncNow, retry, outbox],
  );

  return <OutboxContext.Provider value={value}>{children}</OutboxContext.Provider>;
}

export function useOutbox() {
  const ctx = useContext(OutboxContext);
  if (!ctx) throw new Error('useOutbox must be used inside <OutboxProvider>');
  return ctx;
}
