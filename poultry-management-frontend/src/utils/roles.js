// DEMONSTRATION ONLY. These roles change which buttons and forms the screen shows.
// They are NOT security: the backend has no login and does not check roles.
export const ROLES = [
  { id: 'FARM_PERSONNEL', label: 'Farm Personnel' },
  { id: 'FIELD_OFFICER', label: 'Field Officer' },
  { id: 'SUPERVISOR', label: 'Supervisor' },
  { id: 'MANAGER', label: 'Manager' },
];

const ALL = ['FARM_PERSONNEL', 'FIELD_OFFICER', 'SUPERVISOR', 'MANAGER'];

// Which demo roles may see each action in the UI.
const CAPABILITIES = {
  logMortality: ALL,
  recordFeedUsage: ALL,
  recordWeights: ['FIELD_OFFICER', 'SUPERVISOR', 'MANAGER'],
  addStock: ['FIELD_OFFICER', 'SUPERVISOR', 'MANAGER'],
  manageFlocks: ['FIELD_OFFICER', 'SUPERVISOR', 'MANAGER'],
  manageFarms: ['SUPERVISOR', 'MANAGER'],
  manageFeed: ['SUPERVISOR', 'MANAGER'],
  resolveAlerts: ['SUPERVISOR', 'MANAGER'],
};

export const DEFAULT_ROLE = 'MANAGER';

export function can(role, capability) {
  const allowed = CAPABILITIES[capability];
  return Array.isArray(allowed) && allowed.includes(role);
}

export function roleLabel(roleId) {
  const found = ROLES.find((r) => r.id === roleId);
  return found ? found.label : roleId;
}
