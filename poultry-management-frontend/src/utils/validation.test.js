import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  hasErrors, validateFarm, validateFlock, validateMortality, validateFeedItem, validateStockMovement, validateWeight,
} from './validation.js';

const TODAY = '2026-11-02';
const flock = { currentBirdCount: 95, placementDate: '2026-10-20' };

test('farm: name required', () => {
  assert.ok(validateFarm({ name: '  ' }).name);
  assert.equal(hasErrors(validateFarm({ name: 'Farm A' })), false);
});

test('flock: negative, zero, decimal and future values are rejected', () => {
  const ok = { flockCode: 'BR-1', farmId: '1', birdType: 'Broiler', placementDate: '2026-10-01', initialBirdCount: '100' };
  assert.equal(hasErrors(validateFlock(ok, TODAY)), false);
  assert.ok(validateFlock({ ...ok, initialBirdCount: '-5' }, TODAY).initialBirdCount);
  assert.ok(validateFlock({ ...ok, initialBirdCount: '0' }, TODAY).initialBirdCount);
  assert.ok(validateFlock({ ...ok, initialBirdCount: '10.5' }, TODAY).initialBirdCount);
  assert.ok(validateFlock({ ...ok, placementDate: '2026-11-03' }, TODAY).placementDate);
  assert.ok(validateFlock({ ...ok, farmId: '' }, TODAY).farmId);
});

test('mortality: count must be a positive whole number within the birds alive', () => {
  const ok = { flockId: '1', recordDate: '2026-11-01', count: '5' };
  assert.equal(hasErrors(validateMortality(ok, flock, TODAY)), false);
  assert.ok(validateMortality({ ...ok, count: '0' }, flock, TODAY).count);
  assert.ok(validateMortality({ ...ok, count: '-2' }, flock, TODAY).count);
  assert.ok(validateMortality({ ...ok, count: '2.5' }, flock, TODAY).count);
  assert.ok(validateMortality({ ...ok, count: '96' }, flock, TODAY).count);
  assert.equal(hasErrors(validateMortality({ ...ok, count: '95' }, flock, TODAY)), false);
});

test('mortality: date rules', () => {
  const ok = { flockId: '1', recordDate: '2026-11-01', count: '5' };
  assert.ok(validateMortality({ ...ok, recordDate: '2026-11-03' }, flock, TODAY).recordDate);
  assert.ok(validateMortality({ ...ok, recordDate: '2026-10-19' }, flock, TODAY).recordDate);
  assert.ok(validateMortality({ ...ok, recordDate: '' }, flock, TODAY).recordDate);
  assert.ok(validateMortality({ ...ok, flockId: '' }, flock, TODAY).flockId);
});

test('feed item: threshold needed, at most 3 decimals', () => {
  const ok = { name: 'Grower', feedType: 'Pellets', reorderThreshold: '500', openingQuantity: '2000' };
  assert.equal(hasErrors(validateFeedItem(ok, { creating: true })), false);
  assert.ok(validateFeedItem({ ...ok, reorderThreshold: '' }, { creating: true }).reorderThreshold);
  assert.ok(validateFeedItem({ ...ok, reorderThreshold: '1.2345' }, { creating: true }).reorderThreshold);
  assert.ok(validateFeedItem({ ...ok, openingQuantity: '-1' }, { creating: true }).openingQuantity);
});

test('stock movement: quantity > 0, usage cannot exceed stock', () => {
  const item = { quantityOnHand: 40 };
  assert.equal(hasErrors(validateStockMovement({ quantity: '40' }, { kind: 'usage', item, today: TODAY })), false);
  assert.ok(validateStockMovement({ quantity: '40.5' }, { kind: 'usage', item, today: TODAY }).quantity);
  assert.ok(validateStockMovement({ quantity: '0' }, { kind: 'usage', item, today: TODAY }).quantity);
  assert.ok(validateStockMovement({ quantity: '-1' }, { kind: 'stock-in', item, today: TODAY }).quantity);
  assert.equal(hasErrors(validateStockMovement({ quantity: '500' }, { kind: 'stock-in', item, today: TODAY })), false);
  assert.ok(validateStockMovement({ quantity: '5', movementDate: '2026-11-03' }, { kind: 'usage', item, today: TODAY }).movementDate);
});

test('weight: sensible kg per bird and a sample size', () => {
  const ok = { recordDate: '2026-11-01', averageBirdWeightKg: '1.35', sampledBirdCount: '50' };
  assert.equal(hasErrors(validateWeight(ok, TODAY)), false);
  assert.ok(validateWeight({ ...ok, averageBirdWeightKg: '0' }, TODAY).averageBirdWeightKg);
  assert.ok(validateWeight({ ...ok, averageBirdWeightKg: '25' }, TODAY).averageBirdWeightKg);
  assert.ok(validateWeight({ ...ok, sampledBirdCount: '0' }, TODAY).sampledBirdCount);
});
