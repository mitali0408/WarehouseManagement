package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Thrown when creating a Warehouse would exceed the maximum number of active Warehouses allowed
 * at a given Location.
 */
public class MaxWarehousesPerLocationExceededException extends InvalidWarehouseOperationException {

  public MaxWarehousesPerLocationExceededException(String location, int maxNumberOfWarehouses) {
    super(
        "Location '"
            + location
            + "' already has the maximum of "
            + maxNumberOfWarehouses
            + " active warehouse(s) allowed.");
  }
}
