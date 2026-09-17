package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseHasActiveAssignmentsException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseAssignmentChecker;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArchiveWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;
  @Mock WarehouseAssignmentChecker assignmentChecker;

  private static Warehouse newWarehouse() {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 40;
    warehouse.stock = 10;
    return warehouse;
  }

  @Test
  public void testArchiveSetsArchivedAtAndDelegatesToStore() {
    var warehouse = newWarehouse();
    when(assignmentChecker.hasActiveAssignments("MWH.001")).thenReturn(false);

    new ArchiveWarehouseUseCase(warehouseStore, assignmentChecker).archive(warehouse);

    assertNotNull(warehouse.archivedAt);
    verify(warehouseStore).update(warehouse);
  }

  @Test
  public void testArchiveThrowsWhenActiveAssignmentsExist() {
    var warehouse = newWarehouse();
    when(assignmentChecker.hasActiveAssignments("MWH.001")).thenReturn(true);

    assertThrows(
        WarehouseHasActiveAssignmentsException.class,
        () -> new ArchiveWarehouseUseCase(warehouseStore, assignmentChecker).archive(warehouse));

    verify(warehouseStore, never()).update(any());
  }
}
