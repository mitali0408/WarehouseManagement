package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.InsufficientWarehouseCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidWarehouseDataException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.MaxWarehousesPerLocationExceededException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Validation shared by Warehouse creation and replacement: whether a candidate Warehouse can be
 * placed at a given (already-resolved-valid) Location, and whether it can hold its own declared
 * stock. Kept independent of the Business Unit Code check, since Create and Replace apply that
 * check with opposite meanings (must not exist / must already exist).
 */
@ApplicationScoped
public class WarehouseFeasibilityValidator {

  private final WarehouseStore warehouseStore;

  public WarehouseFeasibilityValidator(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  public void assertRequiredFieldsPresent(Warehouse warehouse) {
    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new InvalidWarehouseDataException("Warehouse businessUnitCode is required.");
    }
    if (warehouse.location == null || warehouse.location.isBlank()) {
      throw new InvalidWarehouseDataException("Warehouse location is required.");
    }
    if (warehouse.capacity == null) {
      throw new InvalidWarehouseDataException("Warehouse capacity is required.");
    }
    if (warehouse.capacity <= 0) {
      throw new InvalidWarehouseDataException("Warehouse capacity must be a positive number.");
    }
    if (warehouse.stock == null) {
      throw new InvalidWarehouseDataException("Warehouse stock is required.");
    }
    if (warehouse.stock < 0) {
      throw new InvalidWarehouseDataException("Warehouse stock cannot be negative.");
    }
  }

  /**
   * Validates that {@code candidate} can be placed at {@code location}, given the currently
   * active warehouses there.
   *
   * @param excludeBusinessUnitCode when non-null, a business unit code to exclude from the
   *     "currently active at this location" calculation — used by Replace to evaluate placement
   *     as though the warehouse being replaced were already gone, without needing to physically
   *     archive it first. This lets Replace validate everything before mutating anything, so a
   *     failed validation never leaves a warehouse archived with no replacement created.
   */
  public void validatePlacement(Warehouse candidate, Location location, String excludeBusinessUnitCode) {
    var activeAtLocation =
        warehouseStore.getByLocation(location.identification).stream()
            .filter(w -> excludeBusinessUnitCode == null || !w.businessUnitCode.equals(excludeBusinessUnitCode))
            .toList();

    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new MaxWarehousesPerLocationExceededException(
          location.identification, location.maxNumberOfWarehouses);
    }

    int totalCapacityAfter =
        activeAtLocation.stream().mapToInt(w -> w.capacity).sum() + candidate.capacity;
    if (totalCapacityAfter > location.maxCapacity) {
      throw new LocationCapacityExceededException(
          location.identification, location.maxCapacity, totalCapacityAfter);
    }

    if (candidate.stock > candidate.capacity) {
      throw new InsufficientWarehouseCapacityException(
          candidate.businessUnitCode, candidate.capacity, candidate.stock);
    }
  }

  /** Convenience overload for Create, where there is no warehouse to exclude. */
  public void validatePlacement(Warehouse candidate, Location location) {
    validatePlacement(candidate, location, null);
  }
}
