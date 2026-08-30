package fr.overwatchtracker.domain;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
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

  @Query(value = """
      SELECT * FROM (
        SELECT DISTINCT ON (battle_tag) *
        FROM player_snapshots
        ORDER BY battle_tag, captured_at DESC
      ) saved
      ORDER BY lower(username), battle_tag
      """, nativeQuery = true)
  List<PlayerSnapshot> findAllDistinct();

  @Query(value = """
      SELECT * FROM (
        SELECT DISTINCT ON (battle_tag) *
        FROM player_snapshots
        WHERE battle_tag <> :battleTag
          AND CASE :role
            WHEN 'tank' THEN tank_rank
            WHEN 'damage' THEN damage_rank
            WHEN 'support' THEN support_rank
          END BETWEEN :minimumRank AND :maximumRank
        ORDER BY battle_tag, captured_at DESC
      ) peers
      ORDER BY captured_at DESC
      """, nativeQuery = true)
  List<PlayerSnapshot> findLatestPeers(
      @Param("battleTag") String battleTag,
      @Param("role") String role,
      @Param("minimumRank") int minimumRank,
      @Param("maximumRank") int maximumRank);

  @Query(value = """
      SELECT CASE :role
        WHEN 'tank' THEN tank_rank
        WHEN 'damage' THEN damage_rank
        WHEN 'support' THEN support_rank
      END
      FROM player_snapshots
      WHERE battle_tag = :battleTag
        AND CASE :role
          WHEN 'tank' THEN tank_rank
          WHEN 'damage' THEN damage_rank
          WHEN 'support' THEN support_rank
        END IS NOT NULL
      ORDER BY captured_at DESC
      LIMIT 1
      """, nativeQuery = true)
  Optional<Integer> findLatestKnownRank(@Param("battleTag") String battleTag, @Param("role") String role);

  Optional<PlayerSnapshot> findFirstByBattleTagOrderByCapturedAtDesc(String battleTag);
  Optional<PlayerSnapshot> findByBattleTagAndSnapshotDate(String battleTag, LocalDate snapshotDate);
}
