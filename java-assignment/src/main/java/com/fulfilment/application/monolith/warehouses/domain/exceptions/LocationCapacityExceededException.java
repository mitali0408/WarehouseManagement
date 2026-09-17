package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Thrown when creating/replacing a Warehouse would push the combined capacity of all active
 * Warehouses at a Location beyond that Location's maximum capacity.
 */
public class LocationCapacityExceededException extends InvalidWarehouseOperationException {

  public LocationCapacityExceededException(String location, int maxCapacity, int attemptedTotal) {
    super(
        "Location '"
            + location
            + "' has a maximum combined capacity of "
            + maxCapacity
            + ", but this operation would result in a total of "
            + attemptedTotal
            + ".");
  }
}
