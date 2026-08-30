ALTER TABLE player_snapshots
  ADD COLUMN snapshot_date DATE;

UPDATE player_snapshots
SET snapshot_date = (captured_at AT TIME ZONE 'Europe/Paris')::date;

DELETE FROM player_snapshots
WHERE id IN (
  SELECT id FROM (
    SELECT id, ROW_NUMBER() OVER (
      PARTITION BY battle_tag, snapshot_date
      ORDER BY captured_at DESC, id DESC
    ) AS position
    FROM player_snapshots
  ) duplicates
  WHERE position > 1
);

ALTER TABLE player_snapshots
  ALTER COLUMN snapshot_date SET NOT NULL;

ALTER TABLE player_snapshots
  ADD CONSTRAINT uq_snapshot_player_day UNIQUE (battle_tag, snapshot_date);
