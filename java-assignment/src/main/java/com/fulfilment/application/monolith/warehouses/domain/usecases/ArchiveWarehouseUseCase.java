package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseHasActiveAssignmentsException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseAssignmentChecker;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final WarehouseAssignmentChecker assignmentChecker;

  public ArchiveWarehouseUseCase(
      WarehouseStore warehouseStore, WarehouseAssignmentChecker assignmentChecker) {
    this.warehouseStore = warehouseStore;
    this.assignmentChecker = assignmentChecker;
  }

  @Override
  public void archive(Warehouse warehouse) {
    // Archiving (unlike Replace) doesn't hand the business unit code to a new active warehouse,
    // so any existing fulfillment assignment referencing this code would be left dangling —
    // pointing at a code with nothing active behind it. Replace is deliberately NOT checked
    // here: it preserves referential validity by keeping the same code active throughout.
    if (assignmentChecker.hasActiveAssignments(warehouse.businessUnitCode)) {
      throw new WarehouseHasActiveAssignmentsException(warehouse.businessUnitCode);
    }

    warehouse.archivedAt = LocalDateTime.now();
    warehouseStore.update(warehouse);
  }
}
