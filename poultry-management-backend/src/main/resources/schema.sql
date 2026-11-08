-- Poultry Management System - SQLite schema (educational prototype)
-- Runs on every startup. Every statement is idempotent (IF NOT EXISTS), so existing data is never touched.
-- Dates are stored as ISO-8601 text ('2026-11-02'); timestamps as UTC text ('2026-11-02T08:30:00.000Z').
-- Text in that format sorts correctly, which is why TEXT is used for dates in SQLite.

CREATE TABLE IF NOT EXISTS farm (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT NOT NULL UNIQUE,
    location      TEXT,
    contact_notes TEXT,
    created_at    TEXT NOT NULL,
    updated_at    TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS flock (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    flock_code         TEXT NOT NULL UNIQUE,
    farm_id            INTEGER NOT NULL REFERENCES farm (id),
    bird_type          TEXT NOT NULL,
    placement_date     TEXT NOT NULL,
    initial_bird_count INTEGER NOT NULL CHECK (initial_bird_count > 0),
    status             TEXT NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED')),
    notes              TEXT,
    created_at         TEXT NOT NULL,
    updated_at         TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_flock_farm ON flock (farm_id);

-- current bird count is NOT stored: it is always initial_bird_count minus the sum of mortality records.
CREATE TABLE IF NOT EXISTS mortality_record (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    flock_id            INTEGER NOT NULL REFERENCES flock (id),
    record_date         TEXT NOT NULL,
    death_count         INTEGER NOT NULL CHECK (death_count > 0),
    reason              TEXT,
    client_operation_id TEXT UNIQUE,
    created_at          TEXT NOT NULL,
    updated_at          TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_mortality_flock_date ON mortality_record (flock_id, record_date);

-- All feed quantities are in kilograms (KG) in this prototype.
CREATE TABLE IF NOT EXISTS feed_item (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    name              TEXT NOT NULL UNIQUE,
    feed_type         TEXT NOT NULL,
    quantity_on_hand  NUMERIC NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    unit              TEXT NOT NULL DEFAULT 'KG' CHECK (unit = 'KG'),
    reorder_threshold NUMERIC NOT NULL DEFAULT 0 CHECK (reorder_threshold >= 0),
    supplier          TEXT,
    notes             TEXT,
    created_at        TEXT NOT NULL,
    updated_at        TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS feed_movement (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    feed_item_id        INTEGER NOT NULL REFERENCES feed_item (id),
    movement_type       TEXT NOT NULL CHECK (movement_type IN ('STOCK_IN', 'USAGE')),
    quantity            NUMERIC NOT NULL CHECK (quantity > 0),
    quantity_after      NUMERIC NOT NULL CHECK (quantity_after >= 0),
    movement_date       TEXT NOT NULL,
    flock_id            INTEGER REFERENCES flock (id),
    notes               TEXT,
    client_operation_id TEXT UNIQUE,
    created_at          TEXT NOT NULL,
    updated_at          TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_movement_item ON feed_movement (feed_item_id);
CREATE INDEX IF NOT EXISTS idx_movement_flock ON feed_movement (flock_id);

CREATE TABLE IF NOT EXISTS weight_record (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    flock_id                INTEGER NOT NULL REFERENCES flock (id),
    record_date             TEXT NOT NULL,
    average_bird_weight_kg  NUMERIC NOT NULL CHECK (average_bird_weight_kg > 0),
    sampled_bird_count      INTEGER NOT NULL CHECK (sampled_bird_count > 0),
    notes                   TEXT,
    created_at              TEXT NOT NULL,
    updated_at              TEXT NOT NULL,
    UNIQUE (flock_id, record_date)
);

CREATE TABLE IF NOT EXISTS alert (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    alert_type          TEXT NOT NULL CHECK (alert_type IN ('HIGH_MORTALITY', 'LOW_STOCK')),
    severity            TEXT NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    status              TEXT NOT NULL CHECK (status IN ('OPEN', 'RESOLVED')),
    message             TEXT NOT NULL,
    related_entity_type TEXT,
    related_entity_id   INTEGER,
    dedup_key           TEXT,
    created_at          TEXT NOT NULL,
    updated_at          TEXT NOT NULL,
    resolved_at         TEXT
);
-- Only ONE open alert may exist per dedup_key (prevents duplicate alerts for the same problem).
CREATE UNIQUE INDEX IF NOT EXISTS uq_alert_open_dedup ON alert (dedup_key) WHERE status = 'OPEN';

CREATE TABLE IF NOT EXISTS app_meta (
    meta_key   TEXT PRIMARY KEY,
    meta_value TEXT NOT NULL
);
