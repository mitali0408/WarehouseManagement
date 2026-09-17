package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Thrown when a Warehouse's capacity is insufficient to hold the stock it is expected to carry —
 * either its own declared stock (on create) or the stock carried over from a Warehouse it is
 * replacing.
 */
public class InsufficientWarehouseCapacityException extends InvalidWarehouseOperationException {

  public InsufficientWarehouseCapacityException(
      String businessUnitCode, int capacity, int requiredStock) {
    super(
        "Warehouse '"
            + businessUnitCode
            + "' has capacity "
            + capacity
            + ", which cannot accommodate a stock of "
            + requiredStock
            + ".");
  }
}
