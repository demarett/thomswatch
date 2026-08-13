package fr.overwatchtracker.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerSnapshotRepository extends JpaRepository<PlayerSnapshot,Long> {
  List<PlayerSnapshot> findTop100ByBattleTagOrderByCapturedAtAsc(String battleTag);
  List<PlayerSnapshot> findTop100ByOrderByCapturedAtDesc();
  Optional<PlayerSnapshot> findFirstByBattleTagOrderByCapturedAtDesc(String battleTag);
}
