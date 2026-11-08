import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../api/endpoints.js';

const ConnectivityContext = createContext(null);
const CHECK_EVERY_MS = 10000;

// "Online" in the browser only means "has a network". What matters is whether OUR BACKEND answers,
// so this asks /api/health every few seconds and when the browser reports a network change.
// apiReachable: null = not checked yet, true = backend answered, false = it did not.
export function ConnectivityProvider({ children }) {
  const [apiReachable, setApiReachable] = useState(null);

  const check = useCallback(async () => {
    try {
      await api.health();
      setApiReachable(true);
    } catch {
      setApiReachable(false);
    }
  }, []);

  useEffect(() => {
    check();
    const timer = setInterval(check, CHECK_EVERY_MS);
    const goOnline = () => check();
    const goOffline = () => setApiReachable(false);
    window.addEventListener('online', goOnline);
    window.addEventListener('offline', goOffline);
    return () => {
      clearInterval(timer);
      window.removeEventListener('online', goOnline);
      window.removeEventListener('offline', goOffline);
    };
  }, [check]);

  const value = useMemo(() => ({ apiReachable, check }), [apiReachable, check]);
  return <ConnectivityContext.Provider value={value}>{children}</ConnectivityContext.Provider>;
}

export function useConnectivity() {
  const ctx = useContext(ConnectivityContext);
  if (!ctx) throw new Error('useConnectivity must be used inside <ConnectivityProvider>');
  return ctx;
}
