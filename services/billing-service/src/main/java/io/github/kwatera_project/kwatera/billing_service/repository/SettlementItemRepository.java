package io.github.kwatera_project.kwatera.billing_service.repository;

import io.github.kwatera_project.kwatera.billing_service.model.SettlementItem;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementItemRepository extends JpaRepository<SettlementItem, UUID> {
  boolean existsBySettlementIdAndTypeIn(UUID settlementId, Collection<SettlementItemType> types);

  List<SettlementItem> findBySettlementId(UUID settlementId);
}
