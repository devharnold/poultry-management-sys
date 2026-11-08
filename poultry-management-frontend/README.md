# Poultry Management System – Frontend (React · Vite)

> **Educational prototype** for the project *"Poultry Management System: A Case of Kenchic"*.
> It is **not** an official Kenchic system and **not** a production-ready platform. Sample data is synthetic and labelled `DEMO`.
> There is **no login**: do not enter confidential data.

This is the web interface. It talks to the REST API in the sibling project `poultry-management-backend` (Spring Boot + SQLite).
**Start the backend first.**

## 1. What you can do

| Page | Purpose |
|---|---|
| **Dashboard** | Active flocks, birds alive, deaths today, low-stock feed, open alerts, recent alerts. Every number is calculated by the backend from stored records. Has a button to load clearly-labelled synthetic demo data when the database is empty. |
| **Farms** | List, search, add, edit farms. |
| **Flocks** | List with search/filters, add, edit. Click a flock code for its detail page. |
| **Flock detail** | Birds alive, mortality history, average-weight records (add one), mortality rate and feed conversion ratio (FCR) with the formulas and a disclaimer. Shows **"Not enough data yet"** instead of guessing. |
| **Mortality** | Record daily deaths (**works offline**), see history and the entries still waiting to sync. |
| **Feed** | Stock per feed item (kg), add stock, record usage (**works offline**), movement history, add/edit feed items. |
| **Alerts** | High-mortality and low-stock alerts (labelled *Prototype-generated*; nothing is sent by SMS/email). Mark as resolved. |
| **Sync** | Every offline entry with its status (**Pending sync / Synced / Sync failed**), retry, discard (with confirmation), storage type, how to test offline. |

The header always shows whether the **backend** is reachable and how many entries are waiting or were rejected.

## 2. Versions

| Item | Version |
|---|---|
| Node.js | **20.19+ or 22.12+** (the current LTS, 22 or 24, is a good choice) – required by Vite 7 |
| npm | the one that comes with Node |
| React | 18.3 |
| Vite | 7.3 with `@vitejs/plugin-react` 5.2 |
| React Router | `react-router-dom` 6.28+ (6.x) |
| Tests | Node's built-in test runner (no extra packages) |

Browsers: any current Chrome, Edge, Firefox or Safari (IndexedDB is used for the offline queue).

## 3. Run it

1. Start the backend (see its README) and check <http://localhost:8080/api/health> says `UP`.
2. In a terminal **inside this folder** (the one containing `package.json`):

```bash
node --version        # must be 20.19+ or 22.12+
npm install           # downloads libraries (first time only; needs internet)
npm run dev
```

3. Open **<http://localhost:5173>** in your browser. Stop with `Ctrl+C`.

Other commands:

| Command | What it does |
|---|---|
| `npm run dev` | Development server on port 5173 (auto-reloads when you edit files) |
| `npm run build` | Production build into `dist/` |
| `npm run preview` | Serves the production build on port 4173 (to use it, add `http://localhost:4173` to the backend's `poultry.cors.allowed-origins`) |
| `npm test` | Runs the unit tests (see §7) |

### Changing the backend address

By default the app calls `http://localhost:8080`. To change it, copy `.env.example` to `.env`, edit `VITE_API_BASE_URL`, and restart `npm run dev`.
The backend only accepts requests from the origins listed in its `poultry.cors.allowed-origins` setting (default `http://localhost:5173` and `http://127.0.0.1:5173`).
The dev server uses `strictPort`, so if port 5173 is busy it stops with an error rather than silently moving to another port (which the backend would then block).

### `package-lock.json`

No lock file is included: the machine that generated this project had no internet access, so `npm install` could not be run there, and a lock file cannot be written by hand.
`npm install` creates it for you the first time. Commit it afterwards so everyone gets identical versions.

## 4. How offline mode works (and its limits)

Mortality and feed-usage entries use a **save first, then send** flow:

1. The entry is stored on **this device** (IndexedDB; falls back to localStorage, then memory) with status **Pending sync**.
2. The app tries to send it. Success → **Synced**.
3. If the server cannot be reached (no connection, timeout, 5xx) it stays **Pending sync** and is retried automatically when the backend answers again, every 30 seconds while something waits, and when you press *sync now*.
4. If the server understood but **refused** the entry (HTTP 400/404/409/422, e.g. more deaths than birds alive) it becomes **Sync failed**, shows the reason, and is **kept** until you retry or discard it. Nothing is dropped silently. (The only automatic clean-up: the list of already-confirmed *Synced* entries keeps the newest 50.)

**No duplicates:** each entry has a UUID that is sent as `clientOperationId`. If an entry is sent twice (for example, the response was lost) the server returns the original record instead of creating another. Entries are sent oldest first.

"Online" is decided by whether **our backend answers** (`/api/health` every 10 s), not just by the browser's network flag.

**Limits (by design for this MVP):**

* Only **mortality** and **feed usage** work offline. Adding feed stock, farms, flocks, feed items and weights need the server and show a clear error if it is unreachable.
* The flock and feed lists shown while offline are the **last copy saved on this device** and are labelled as such; birds-alive and stock numbers may be out of date. The server re-checks every rule when the entry is finally sent.
* After a **reload while the server is down**, a page can only show lists that were saved on this device earlier; pages never visited online will show an error until the server is back. Pending entries are never lost by a reload.
* There is **no conflict resolution** for simultaneous edits from several people; retries are safe, edits are "last save wins".
* If the browser cannot provide permanent storage (some private-browsing modes) the Sync page warns that entries are kept in memory only.

### Try it yourself (about 2 minutes)

1. Open **Mortality**, wait for the flock list.
2. Stop the backend (`Ctrl+C` in its terminal). Within ~10 s the header says *Server unreachable*.
3. Record deaths → *Pending sync*. Reload the page → the entry is still on the **Sync** page.
4. Start the backend again → it becomes *Synced* by itself. Check the Mortality history: exactly one record.

## 5. Demo roles (not security!)

The role picker in the header (Farm Personnel, Field Officer, Supervisor, Manager) only changes which buttons and forms are shown, for demonstrating role-aware screens.
**It is not authentication or authorization**: the backend has no login and does not check roles, and anyone can pick any role. The default is *Manager* (sees everything).

| Action shown | Roles |
|---|---|
| Record mortality, record feed usage | all roles |
| Record weights, add feed stock, create/edit flocks | Field Officer, Supervisor, Manager |
| Create/edit farms, create/edit feed items, resolve alerts | Supervisor, Manager |

## 6. Project structure

```
src/
  main.jsx, App.jsx            entry point and routes
  api/        client.js (fetch wrapper + ApiError)   endpoints.js (every backend URL)
              outbox.js (offline queue logic, no React)   outboxStorage.js (IndexedDB / localStorage / memory)
  hooks/      useApi (loading data, with cached fallback)  useConnectivity  useOutbox  useRole
  components/ forms (FarmForm, FlockForm, MortalityForm, FeedUsageForm, StockInForm, FeedItemForm, WeightForm),
              Layout, SyncBadge, RoleSwitcher, AlertItem, OutboxList, Field, Notice, StatCard
  pages/      Dashboard, Farms, Flocks, FlockDetail, Mortality, Feed, Alerts, Sync, NotFound
  utils/      validation.js (browser-side checks)  roles.js  format.js  uuid.js  cache.js
  styles/     app.css (plain CSS, responsive)
```

Browser-side validation is only for convenience (it blocks empty, negative, zero, decimal-where-whole, future-date and too-many-deaths input before sending).
**The backend repeats every rule and has the final say.**

## 7. Tests and actual known test status (read this)

`npm test` runs `node --test` over these files (no extra packages needed):

* `src/api/outbox.test.js` – the offline queue: offline keeps entries, order, no hammering, retry after lost response doesn't duplicate, rejection → failed and kept, discard, concurrent syncs, entries added mid-sync.
* `src/utils/validation.test.js` – every form rule, including negative/zero/decimal/future cases.
* `src/utils/misc.test.js` – UUID generation (incl. the no-`randomUUID` fallback), demo roles, API client error handling.

### What was actually run

The environment this project was built in had **no internet access**, so `npm install`, `npm run dev`, `npm run build` and `npm test` **were not run** as you will run them. Verified instead:

| Check | Result | Limitation |
|---|---|---|
| The 28 unit tests above, run with Node's test runner | ✅ 28/28 passed | These are the same files shipped here. |
| All 42 source files compile (syntax + JSX) with esbuild; all 135 relative imports and 199 imported names resolve | ✅ passed | esbuild is not Vite. |
| The real app source, bundled and loaded in **headless Chromium**, driven through a scenario (28 checks, repeated 4 times, all passed): dashboard, validation, online save, **server stopped → entry "Pending sync" → page reload → entry still in real IndexedDB → server restarted → auto-sync → exactly one record**, replay of an id, server-side rejection → "Sync failed" → discard, role gating, feed list, 404 page, no console errors | ✅ passed | Used a **test-only stand-in for `react-router-dom`** (not installable offline), **React 19** (the project pins 18.3), and a **mock API written to the documented contract**, not the real Spring backend. So routing under the real router, React 18, and the real backend contract were **not** exercised together. |
| Visual check of desktop and 390 px phone widths | ✅ looked correct; one low-contrast text defect found and fixed | Screenshots taken in Chromium only. |
| `npm run dev` / `npm run build` / real `react-router-dom` / real backend end-to-end | ⚠️ **not run** | Please run them; if something fails, treat it as a real bug. |
| Accessibility audit with a screen reader, real mobile devices, Safari/Firefox | ⚠️ **not done** | Built with labels, `aria-live` status, visible focus, text (not just colour) for status, and an accessible skip link, but unverified by assistive-technology testing. |

### Manual test checklist (about 10 minutes, with the real backend)

| # | Do this | Expect |
|---|---|---|
| 1 | Empty database, open Dashboard | "No data yet" with *Add a farm* and *Load synthetic demo data* |
| 2 | Add a farm, then a flock (try a negative bird count) | Negative is rejected with a clear message |
| 3 | Add a flock with an existing code | Error under *Flock/batch code* |
| 4 | Mortality: record 5 deaths | "Saved on the server"; flock shows 5 fewer birds alive |
| 5 | Mortality: more deaths than birds alive | Rejected in the browser; the backend also rejects it |
| 6 | Record deaths ≥ the alert threshold in one day | An alert appears (Alerts page and Dashboard) |
| 7 | Feed: add an item with stock, record usage above the stock | Rejected, quantity unchanged |
| 8 | Feed: use stock down to the reorder level | *Low stock* badge and an alert; adding stock resolves it |
| 9 | Offline test (§4) | Pending → survives reload → synced once |
| 10 | Load demo data, open flock DEMO-001 | FCR 1.63 with formula; DEMO-002 shows "Not enough data yet" |
| 11 | Restart backend and reload | All data still there |
| 12 | Resize the window to phone width | Tables become cards; nothing is cut off |

## 8. Troubleshooting

| Problem | What to do |
|---|---|
| `npm: command not found` / wrong Node version | Install Node LTS from nodejs.org, open a **new** terminal, check `node --version`. |
| `npm install` fails | Check your internet/proxy; delete `node_modules` and retry. |
| `Port 5173 is already in use` | Close the other program (or the other `npm run dev`). Do not use another port unless you also add it to the backend's `poultry.cors.allowed-origins`. |
| Header says **Server unreachable** | The backend is not running (or not on port 8080). Start it and check `/api/health`. |
| Browser console shows a **CORS** error | Open the app at exactly `http://localhost:5173` (or add your address to `poultry.cors.allowed-origins` in the backend and restart it). Opening it from a phone via `http://192.168.x.x:5173` needs that address added too, and `npm run dev -- --host`. |
| Page is blank | Open the browser developer console (F12) and read the first red error. |
| Lists look out of date while offline | They are the last saved copy (labelled). They refresh when the server is reachable. |
| An entry says **Sync failed** | Read the reason, then *Try again* (if the cause is fixed) or *Discard…*. |

## 9. Limitations

No authentication or real roles; no SMS/email; mortality and feed movements cannot be edited or deleted; only mortality and feed usage work offline;
no conflict resolution for simultaneous edits; thresholds and indicators are unvalidated placeholders; no user study or SUS result is included or claimed
(if your project needs one, collect real participant responses and calculate SUS from them).
