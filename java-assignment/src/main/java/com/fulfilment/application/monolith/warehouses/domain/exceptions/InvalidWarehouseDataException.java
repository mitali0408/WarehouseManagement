package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** Thrown when a Warehouse is submitted without the required fields populated. */
public class InvalidWarehouseDataException extends InvalidWarehouseOperationException {

  public InvalidWarehouseDataException(String message) {
    super(message);
  }
}
