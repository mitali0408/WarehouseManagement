package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.InsufficientWarehouseCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseStockMismatchException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;
  private final WarehouseFeasibilityValidator feasibilityValidator;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore,
      LocationResolver locationResolver,
      WarehouseFeasibilityValidator feasibilityValidator) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
    this.feasibilityValidator = feasibilityValidator;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    feasibilityValidator.assertRequiredFieldsPresent(newWarehouse);

    // Business Unit Code Verification (replace semantics): the code must already identify the
    // currently active warehouse being replaced.
    Warehouse previousWarehouse =
        warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (previousWarehouse == null) {
      throw new WarehouseNotFoundException(newWarehouse.businessUnitCode);
    }

    // Location Validation.
    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);

    // Stock Matching: new warehouse's stock must match the warehouse being replaced.
    if (!newWarehouse.stock.equals(previousWarehouse.stock)) {
      throw new WarehouseStockMismatchException(
          newWarehouse.businessUnitCode, previousWarehouse.stock, newWarehouse.stock);
    }

    // Capacity Accommodation: new capacity must be able to hold the stock carried over.
    if (newWarehouse.capacity < previousWarehouse.stock) {
      throw new InsufficientWarehouseCapacityException(
          newWarehouse.businessUnitCode, newWarehouse.capacity, previousWarehouse.stock);
    }

    // Warehouse Creation Feasibility + Capacity and Stock Validation — evaluated as though the
    // outgoing warehouse were already gone (excluded by code), without having actually archived
    // it yet. Every validation above and below this line completes before any mutation happens,
    // so this use case is safe to call on its own: a failed validation never leaves the outgoing
    // warehouse archived with no replacement created, regardless of whether an ambient
    // transaction happens to be present to roll it back.
    feasibilityValidator.validatePlacement(newWarehouse, location, previousWarehouse.businessUnitCode);

    // All validation has passed — now, and only now, mutate.
    previousWarehouse.archivedAt = LocalDateTime.now();
    warehouseStore.update(previousWarehouse);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;

    warehouseStore.create(newWarehouse);
  }
}
