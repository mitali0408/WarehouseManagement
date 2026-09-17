package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Base type for domain failures that mean the requested Warehouse operation is invalid as
 * submitted (a business rule was violated, or a referenced entity such as a Location does not
 * exist). Adapters map this whole family to a single transport-level response (HTTP 400), rather
 * than needing a catch block per concrete subtype.
 */
public abstract class InvalidWarehouseOperationException extends RuntimeException {

  protected InvalidWarehouseOperationException(String message) {
    super(message);
  }
}
