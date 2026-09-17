package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseAssignmentChecker;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FulfillmentWarehouseAssignmentChecker implements WarehouseAssignmentChecker {

  private final FulfillmentAssignmentRepository repository;

  public FulfillmentWarehouseAssignmentChecker(FulfillmentAssignmentRepository repository) {
    this.repository = repository;
  }

  @Override
  public boolean hasActiveAssignments(String businessUnitCode) {
    return repository.count("warehouseBusinessUnitCode", businessUnitCode) > 0;
  }
}
