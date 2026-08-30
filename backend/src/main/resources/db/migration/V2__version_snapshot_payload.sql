ALTER TABLE player_snapshots
  ALTER COLUMN payload TYPE JSONB USING payload::JSONB;

ALTER TABLE player_snapshots
  ADD COLUMN payload_version INTEGER NOT NULL DEFAULT 1
  CHECK (payload_version > 0);

ALTER TABLE player_snapshots
  ALTER COLUMN payload_version DROP DEFAULT;
