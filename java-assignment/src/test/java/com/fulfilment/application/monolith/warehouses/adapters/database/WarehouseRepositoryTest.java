package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.BusinessUnitCodeAlreadyExistsException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Direct repository-level tests, covering paths that are hard to reach through the use-case layer
 * alone: the database-level uniqueness safety net specifically (bypassing
 * CreateWarehouseUseCase's own check, to simulate the race condition it guards against), and the
 * not-found paths in update()/remove() when called with a code that doesn't resolve to any
 * active row.
 */
@QuarkusTest
public class WarehouseRepositoryTest {

  @Inject WarehouseRepository warehouseRepository;

  private static Warehouse newWarehouse(String code, String location, int capacity, int stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    warehouse.createdAt = LocalDateTime.now();
    return warehouse;
  }

  @Test
  @TestTransaction
  public void testCreate_databaseConstraintCatchesDuplicateBypassingUseCaseCheck() {
    warehouseRepository.create(newWarehouse("TEST.REPO.DUP", "HELMOND-001", 10, 0));

    // calling the repository directly a second time with the same code, bypassing
    // CreateWarehouseUseCase's own findByBusinessUnitCode check entirely — this is exactly the
    // race-condition scenario the activeBusinessUnitCode unique constraint exists to catch
    assertThrows(
        BusinessUnitCodeAlreadyExistsException.class,
        () -> warehouseRepository.create(newWarehouse("TEST.REPO.DUP", "HELMOND-001", 20, 0)));
  }

  @Test
  @TestTransaction
  public void testUpdate_throwsWhenCodeNotActive() {
    var warehouse = newWarehouse("NOT.AN.ACTIVE.CODE", "HELMOND-001", 10, 0);

    assertThrows(WarehouseNotFoundException.class, () -> warehouseRepository.update(warehouse));
  }

  @Test
  @TestTransaction
  public void testRemove_successAndNotFound() {
    warehouseRepository.create(newWarehouse("TEST.REPO.REMOVE", "HELMOND-001", 10, 0));

    assertEquals(
        "TEST.REPO.REMOVE",
        warehouseRepository.findByBusinessUnitCode("TEST.REPO.REMOVE").businessUnitCode);

    warehouseRepository.remove(newWarehouse("TEST.REPO.REMOVE", "HELMOND-001", 10, 0));

    assertNull(warehouseRepository.findByBusinessUnitCode("TEST.REPO.REMOVE"));
  }

  @Test
  @TestTransaction
  public void testRemove_throwsWhenCodeNotActive() {
    var warehouse = newWarehouse("NOT.AN.ACTIVE.CODE", "HELMOND-001", 10, 0);

    assertThrows(WarehouseNotFoundException.class, () -> warehouseRepository.remove(warehouse));
  }
}
