package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Thrown when attempting to create a Warehouse under a Business Unit Code that already
 * identifies a currently active Warehouse.
 */
public class BusinessUnitCodeAlreadyExistsException extends InvalidWarehouseOperationException {

  public BusinessUnitCodeAlreadyExistsException(String businessUnitCode) {
    super(
        "A Warehouse with business unit code '"
            + businessUnitCode
            + "' already exists and is active.");
  }
}
