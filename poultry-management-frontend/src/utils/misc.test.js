import { test } from 'node:test';
import assert from 'node:assert/strict';
import { newId } from './uuid.js';
import { can, ROLES } from './roles.js';
import { todayIso, formatDate } from './format.js';
import { apiFetch, ApiError } from '../api/client.js';

test('newId makes a valid, unique id that the backend will accept (8-64 letters/digits/hyphens)', () => {
  const a = newId();
  const b = newId();
  assert.notEqual(a, b);
  assert.match(a, /^[A-Za-z0-9-]{8,64}$/);
  assert.match(a, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
});

test('newId fallback works when crypto.randomUUID is missing (plain-http pages)', () => {
  const original = Object.getOwnPropertyDescriptor(globalThis, 'crypto');
  Object.defineProperty(globalThis, 'crypto', { value: { getRandomValues: (a) => { a.fill(7); return a; } }, configurable: true });
  try {
    assert.match(newId(), /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
  } finally {
    if (original) Object.defineProperty(globalThis, 'crypto', original);
  }
});

test('demo roles gate actions (UI only)', () => {
  assert.equal(ROLES.length, 4);
  assert.equal(can('FARM_PERSONNEL', 'logMortality'), true);
  assert.equal(can('FARM_PERSONNEL', 'manageFarms'), false);
  assert.equal(can('FARM_PERSONNEL', 'resolveAlerts'), false);
  assert.equal(can('SUPERVISOR', 'resolveAlerts'), true);
  assert.equal(can('MANAGER', 'manageFeed'), true);
  assert.equal(can('MANAGER', 'doesNotExist'), false);
});

test('date helpers', () => {
  assert.equal(todayIso(new Date(2026, 10, 2)), '2026-11-02');
  assert.equal(formatDate('2026-11-02'), '02/11/2026');
});

test('apiFetch: success, JSON error body, non-JSON error body, network failure', async () => {
  const reply = (status, body) => async () => ({ ok: status < 400, status, text: async () => body });
  assert.deepEqual(await apiFetch('/x', { fetchImpl: reply(200, '{"a":1}') }), { a: 1 });
  assert.equal(await apiFetch('/x', { fetchImpl: reply(204, '') }), null);

  await assert.rejects(apiFetch('/x', { fetchImpl: reply(422, '{"message":"Too many","fieldErrors":{"count":"Too many"}}') }),
    (e) => e instanceof ApiError && e.status === 422 && e.fieldErrors.count === 'Too many' && !e.isTransient);
  await assert.rejects(apiFetch('/x', { fetchImpl: reply(502, '<html>bad gateway</html>') }),
    (e) => e.status === 502 && e.isTransient && /HTTP 502/.test(e.message));
  await assert.rejects(apiFetch('/x', { fetchImpl: async () => { throw new TypeError('Failed to fetch'); } }),
    (e) => e.isNetworkError && e.isTransient);
});

test('apiFetch sends JSON with the right headers', async () => {
  let seen;
  await apiFetch('/api/mortality', { method: 'POST', body: { count: 1 }, fetchImpl: async (url, init) => { seen = { url, init }; return { ok: true, status: 201, text: async () => '{}' }; } });
  assert.equal(seen.url, 'http://localhost:8080/api/mortality');
  assert.equal(seen.init.method, 'POST');
  assert.equal(seen.init.headers['Content-Type'], 'application/json');
  assert.equal(seen.init.body, '{"count":1}');
});
