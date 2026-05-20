package io.github.kwatera_project.kwatera.property_service.repository;

import io.github.kwatera_project.kwatera.property_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.property_service.model.UnitSettlementItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitSettlementItemRepository extends JpaRepository<UnitSettlementItem, UUID> {

  List<UnitSettlementItem> findByUnitId(UUID unitId);

  List<UnitSettlementItem> findByUnitIdAndSettlementItemType(
      UUID unitId, SettlementItemType settlementItemType);
}
