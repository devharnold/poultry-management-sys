import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { DEFAULT_ROLE, ROLES, can as canRole } from '../utils/roles.js';

const RoleContext = createContext(null);
const KEY = 'poultry.demoRole';

// DEMONSTRATION ONLY: remembers which demo role the visitor picked. This is not login or security.
export function RoleProvider({ children }) {
  const [role, setRoleState] = useState(() => {
    try {
      const saved = localStorage.getItem(KEY);
      return ROLES.some((r) => r.id === saved) ? saved : DEFAULT_ROLE;
    } catch {
      return DEFAULT_ROLE;
    }
  });

  const setRole = useCallback((next) => {
    setRoleState(next);
    try {
      localStorage.setItem(KEY, next);
    } catch {
      // ignore: remembering the role is optional
    }
  }, []);

  const value = useMemo(() => ({ role, setRole, can: (capability) => canRole(role, capability) }), [role, setRole]);
  return <RoleContext.Provider value={value}>{children}</RoleContext.Provider>;
}

export function useRole() {
  const ctx = useContext(RoleContext);
  if (!ctx) throw new Error('useRole must be used inside <RoleProvider>');
  return ctx;
}
