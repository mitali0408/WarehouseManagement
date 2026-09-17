package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.BusinessUnitCodeAlreadyExistsException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;
  private final WarehouseFeasibilityValidator feasibilityValidator;

  public CreateWarehouseUseCase(
      WarehouseStore warehouseStore,
      LocationResolver locationResolver,
      WarehouseFeasibilityValidator feasibilityValidator) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
    this.feasibilityValidator = feasibilityValidator;
  }

  @Override
  public void create(Warehouse warehouse) {
    feasibilityValidator.assertRequiredFieldsPresent(warehouse);

    // Business Unit Code Verification: must not already identify an active warehouse.
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new BusinessUnitCodeAlreadyExistsException(warehouse.businessUnitCode);
    }

    // Location Validation: resolveByIdentifier throws LocationNotFoundException if invalid.
    Location location = locationResolver.resolveByIdentifier(warehouse.location);

    // Warehouse Creation Feasibility + Capacity and Stock Validation.
    feasibilityValidator.validatePlacement(warehouse, location);

    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;

    warehouseStore.create(warehouse);
  }
}
