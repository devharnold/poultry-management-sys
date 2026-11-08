// Friendly checks that run in the browser BEFORE sending data. They are for convenience only:
// the backend repeats every rule and has the final say.
// Each function returns an object like { fieldName: 'message' } (empty object = no problems).

export const hasErrors = (errors) => Object.keys(errors).length > 0;

const isBlank = (v) => v === undefined || v === null || String(v).trim() === '';
const isWholeNumber = (v) => /^\d+$/.test(String(v).trim());
const isDecimal3 = (v) => /^\d+(\.\d{1,3})?$/.test(String(v).trim());

export function validateFarm(v) {
  const e = {};
  if (isBlank(v.name)) e.name = 'Farm name is required.';
  else if (v.name.trim().length > 120) e.name = 'Farm name must be at most 120 characters.';
  return e;
}

export function validateFlock(v, today) {
  const e = {};
  if (isBlank(v.flockCode)) e.flockCode = 'Flock/batch code is required.';
  else if (v.flockCode.trim().length > 40) e.flockCode = 'Code must be at most 40 characters.';
  if (isBlank(v.farmId)) e.farmId = 'Please choose a farm.';
  if (isBlank(v.birdType)) e.birdType = 'Bird type/breed is required.';
  if (isBlank(v.placementDate)) e.placementDate = 'Placement date is required.';
  else if (v.placementDate > today) e.placementDate = 'Placement date cannot be in the future.';
  if (isBlank(v.initialBirdCount)) e.initialBirdCount = 'Initial bird count is required.';
  else if (!isWholeNumber(v.initialBirdCount) || Number(v.initialBirdCount) < 1)
    e.initialBirdCount = 'Enter a whole number of at least 1 (no negatives).';
  else if (Number(v.initialBirdCount) > 1000000) e.initialBirdCount = 'That number looks too large.';
  return e;
}

// flock = the selected flock object from the API (may be undefined while loading)
export function validateMortality(v, flock, today) {
  const e = {};
  if (isBlank(v.flockId)) e.flockId = 'Please choose a flock.';
  if (isBlank(v.recordDate)) e.recordDate = 'Date is required.';
  else if (v.recordDate > today) e.recordDate = 'Date cannot be in the future.';
  else if (flock && flock.placementDate && v.recordDate < flock.placementDate)
    e.recordDate = `Date cannot be before the flock's placement date (${flock.placementDate}).`;
  if (isBlank(v.count)) e.count = 'Number of deaths is required.';
  else if (!isWholeNumber(v.count) || Number(v.count) < 1) e.count = 'Enter a whole number of at least 1.';
  else if (flock && Number(v.count) > flock.currentBirdCount)
    e.count = `Only ${flock.currentBirdCount} birds are alive in this flock (as last loaded).`;
  if (v.reason && v.reason.length > 500) e.reason = 'Reason must be at most 500 characters.';
  return e;
}

export function validateFeedItem(v, { creating }) {
  const e = {};
  if (isBlank(v.name)) e.name = 'Feed name is required.';
  if (isBlank(v.feedType)) e.feedType = 'Feed type is required.';
  if (isBlank(v.reorderThreshold)) e.reorderThreshold = 'Reorder threshold is required (use 0 for none).';
  else if (!isDecimal3(v.reorderThreshold)) e.reorderThreshold = 'Use a number in kg with at most 3 decimals.';
  if (creating && !isBlank(v.openingQuantity) && !isDecimal3(v.openingQuantity))
    e.openingQuantity = 'Use a number in kg with at most 3 decimals.';
  return e;
}

// kind: 'usage' | 'stock-in'; item = selected feed item (for the "not enough stock" check)
export function validateStockMovement(v, { kind, item, today }) {
  const e = {};
  if (isBlank(v.quantity)) e.quantity = 'Quantity is required.';
  else if (!isDecimal3(v.quantity) || Number(v.quantity) <= 0)
    e.quantity = 'Enter a quantity in kg greater than 0 (at most 3 decimals).';
  else if (kind === 'usage' && item && Number(v.quantity) > Number(item.quantityOnHand))
    e.quantity = `Only ${item.quantityOnHand} kg is in stock (as last loaded).`;
  if (!isBlank(v.movementDate) && v.movementDate > today) e.movementDate = 'Date cannot be in the future.';
  return e;
}

export function validateWeight(v, today) {
  const e = {};
  if (isBlank(v.recordDate)) e.recordDate = 'Date is required.';
  else if (v.recordDate > today) e.recordDate = 'Date cannot be in the future.';
  if (isBlank(v.averageBirdWeightKg)) e.averageBirdWeightKg = 'Average weight is required.';
  else if (!isDecimal3(v.averageBirdWeightKg) || Number(v.averageBirdWeightKg) <= 0)
    e.averageBirdWeightKg = 'Enter kg per bird, greater than 0 (at most 3 decimals).';
  else if (Number(v.averageBirdWeightKg) > 20) e.averageBirdWeightKg = 'That weight looks too large for one bird.';
  if (isBlank(v.sampledBirdCount)) e.sampledBirdCount = 'Sample size is required.';
  else if (!isWholeNumber(v.sampledBirdCount) || Number(v.sampledBirdCount) < 1)
    e.sampledBirdCount = 'Enter a whole number of at least 1.';
  return e;
}
