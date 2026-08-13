CREATE TABLE player_snapshots (
  id BIGSERIAL PRIMARY KEY,
  battle_tag VARCHAR(80) NOT NULL,
  captured_at TIMESTAMPTZ NOT NULL,
  username VARCHAR(80) NOT NULL,
  platform VARCHAR(20),
  total_time_played BIGINT,
  win_rate DOUBLE PRECISION,
  tank_rank INTEGER,
  damage_rank INTEGER,
  support_rank INTEGER,
  payload JSONB NOT NULL,
  payload_version INTEGER NOT NULL CHECK (payload_version > 0)
);
CREATE INDEX idx_snapshots_battletag_date ON player_snapshots (battle_tag, captured_at DESC);
