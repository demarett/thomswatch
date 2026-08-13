package fr.overwatchtracker.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerSnapshotRepository extends JpaRepository<PlayerSnapshot, Long> {
  @Query(value = """
      SELECT * FROM player_snapshots
      WHERE battle_tag = :battleTag
      ORDER BY captured_at DESC
      LIMIT 100
      """, nativeQuery = true)
  List<PlayerSnapshot> findLatestHistory(@Param("battleTag") String battleTag);

  @Query(value = """
      SELECT * FROM (
        SELECT DISTINCT ON (battle_tag) *
        FROM player_snapshots
        ORDER BY battle_tag, captured_at DESC
      ) recent
      ORDER BY captured_at DESC
      LIMIT 8
      """, nativeQuery = true)
  List<PlayerSnapshot> findRecentDistinct();

  Optional<PlayerSnapshot> findFirstByBattleTagOrderByCapturedAtDesc(String battleTag);
}
