import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createOutbox, STATUS } from './outbox.js';
import { createMemoryStorage } from './outboxStorage.js';
import { ApiError } from './client.js';

// A fake "server" the outbox sends to. It behaves like the real backend: it applies each id only once.
function makeHarness() {
  let counter = 0;
  let clock = 0;
  const server = { online: true, records: new Map(), rejectWith: null, calls: 0 };
  const send = async (entry) => {
    server.calls += 1;
    if (!server.online) throw new ApiError('Cannot reach the server.', { isNetworkError: true });
    if (server.rejectWith) throw new ApiError(server.rejectWith, { status: 422, fieldErrors: { count: server.rejectWith } });
    if (!server.records.has(entry.id)) server.records.set(entry.id, entry.payload); // idempotent
  };
  const outbox = createOutbox({
    storage: createMemoryStorage(),
    send,
    newId: () => `id-${(counter += 1)}`,
    now: () => new Date(Date.UTC(2026, 10, 2, 8, 0, (clock += 1))).toISOString(),
  });
  return { outbox, server };
}

const mortality = (n) => ({ type: 'mortality', payload: { flockId: 1, count: n }, label: `deaths ${n}` });

test('online: entry is saved then synced, and the server got exactly one record', async () => {
  const { outbox, server } = makeHarness();
  await outbox.add(mortality(5));
  const result = await outbox.syncAll();
  assert.deepEqual(result, { synced: 1, failed: 0, pending: 0 });
  assert.equal(server.records.size, 1);
  assert.equal((await outbox.list())[0].status, STATUS.SYNCED);
});

test('offline: entry stays pending (not lost) and keeps its id', async () => {
  const { outbox, server } = makeHarness();
  server.online = false;
  const entry = await outbox.add(mortality(5));
  const result = await outbox.syncAll();
  assert.equal(result.pending, 1);
  const stored = (await outbox.list())[0];
  assert.equal(stored.status, STATUS.PENDING);
  assert.equal(stored.id, entry.id);
  assert.equal(stored.attempts, 1);
  assert.match(stored.lastError, /Cannot reach/);
  assert.equal(server.records.size, 0);
});

test('reconnect: pending entries are sent in the order they were created', async () => {
  const { outbox, server } = makeHarness();
  server.online = false;
  await outbox.add(mortality(1));
  await outbox.add(mortality(2));
  await outbox.add(mortality(3));
  await outbox.syncAll();
  server.online = true;
  const result = await outbox.syncAll();
  assert.deepEqual(result, { synced: 3, failed: 0, pending: 0 });
  assert.deepEqual([...server.records.values()].map((p) => p.count), [1, 2, 3]);
});

test('while offline, only ONE attempt is made per sync (no hammering)', async () => {
  const { outbox, server } = makeHarness();
  server.online = false;
  await outbox.add(mortality(1));
  await outbox.add(mortality(2));
  await outbox.syncAll();
  assert.equal(server.calls, 1);
});

test('re-sending an entry the server already has (lost response) does not create a duplicate', async () => {
  // The server stores the record but the browser never hears back (network drops): the entry stays pending
  // and is sent AGAIN later with the same id. The fake server, like the real one, applies each id once.
  let counter = 0;
  const records = new Map();
  let dropReply = true;
  const send = async (entry) => {
    if (!records.has(entry.id)) records.set(entry.id, entry.payload);
    if (dropReply) throw new ApiError('Cannot reach the server.', { isNetworkError: true });
  };
  const outbox = createOutbox({ storage: createMemoryStorage(), send, newId: () => `id-${(counter += 1)}` });
  await outbox.add(mortality(5));
  await outbox.syncAll();
  assert.equal((await outbox.list())[0].status, STATUS.PENDING);
  dropReply = false;
  await outbox.syncAll();
  assert.equal(records.size, 1);
  assert.equal((await outbox.list())[0].status, STATUS.SYNCED);
});

test('server rejection (422) becomes "failed" with the reason and is kept, not dropped', async () => {
  const { outbox, server } = makeHarness();
  server.rejectWith = 'Only 3 birds are alive';
  await outbox.add(mortality(99));
  const result = await outbox.syncAll();
  assert.deepEqual(result, { synced: 0, failed: 1, pending: 0 });
  const stored = (await outbox.list())[0];
  assert.equal(stored.status, STATUS.FAILED);
  assert.equal(stored.lastError, 'Only 3 birds are alive');
  assert.equal(stored.fieldErrors.count, 'Only 3 birds are alive');
});

test('failed entries are not retried automatically, only when the user asks', async () => {
  const { outbox, server } = makeHarness();
  server.rejectWith = 'nope';
  const entry = await outbox.add(mortality(99));
  await outbox.syncAll();
  const callsAfterFirst = server.calls;
  await outbox.syncAll();
  assert.equal(server.calls, callsAfterFirst);
  server.rejectWith = null;
  const result = await outbox.retry(entry.id);
  assert.equal(result.synced, 1);
  assert.equal((await outbox.get(entry.id)).status, STATUS.SYNCED);
});

test('a failed entry does not block the entries after it', async () => {
  const { outbox, server } = makeHarness();
  server.rejectWith = 'nope';
  await outbox.add(mortality(1));
  await outbox.syncAll();
  server.rejectWith = null;
  await outbox.add(mortality(2));
  const result = await outbox.syncAll();
  assert.equal(result.synced, 1);
  const statuses = (await outbox.list()).map((e) => e.status);
  assert.deepEqual(statuses, [STATUS.FAILED, STATUS.SYNCED]);
});

test('discard removes only the chosen entry', async () => {
  const { outbox, server } = makeHarness();
  server.online = false;
  const a = await outbox.add(mortality(1));
  await outbox.add(mortality(2));
  await outbox.discard(a.id);
  assert.equal((await outbox.list()).length, 1);
});

test('an entry discarded while it is being sent is not brought back to life', async () => {
  const { outbox } = makeHarness();
  const entry = await outbox.add(mortality(1));
  const sync = outbox.syncAll();
  await outbox.discard(entry.id);
  await sync;
  assert.equal((await outbox.list()).length, 0);
});

test('an entry added while a sync is running is still sent', async () => {
  const { outbox, server } = makeHarness();
  await outbox.add(mortality(1));
  const first = outbox.syncAll();
  await outbox.add(mortality(2));
  const second = outbox.syncAll();
  await Promise.all([first, second]);
  assert.equal(server.records.size, 2);
  assert.ok((await outbox.list()).every((e) => e.status === STATUS.SYNCED));
});

test('two syncAll calls at once do not send the same entry twice', async () => {
  const { outbox, server } = makeHarness();
  await outbox.add(mortality(1));
  await Promise.all([outbox.syncAll(), outbox.syncAll()]);
  assert.equal(server.calls, 1);
});

test('clearSynced removes only synced entries', async () => {
  const { outbox, server } = makeHarness();
  await outbox.add(mortality(1));
  await outbox.syncAll();
  server.online = false;
  await outbox.add(mortality(2));
  await outbox.syncAll();
  await outbox.clearSynced();
  const left = await outbox.list();
  assert.equal(left.length, 1);
  assert.equal(left[0].status, STATUS.PENDING);
});

test('subscribers are told about changes', async () => {
  const { outbox } = makeHarness();
  let calls = 0;
  const off = outbox.subscribe(() => { calls += 1; });
  await outbox.add(mortality(1));
  assert.ok(calls > 0);
  off();
  const before = calls;
  await outbox.add(mortality(2));
  assert.equal(calls, before);
});

test('ApiError.isTransient classifies status codes correctly', () => {
  assert.equal(new ApiError('x', { isNetworkError: true }).isTransient, true);
  assert.equal(new ApiError('x', { status: 503 }).isTransient, true);
  assert.equal(new ApiError('x', { status: 429 }).isTransient, true);
  for (const status of [400, 404, 409, 422]) assert.equal(new ApiError('x', { status }).isTransient, false);
});
