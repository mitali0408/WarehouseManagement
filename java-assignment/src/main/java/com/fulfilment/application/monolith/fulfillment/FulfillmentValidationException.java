package com.fulfilment.application.monolith.fulfillment;

/**
 * Base type for fulfillment-assignment business rule violations (duplicate assignment, or any of
 * the three quantity constraints). Mapped centrally to HTTP 400 by {@link
 * FulfillmentValidationExceptionMapper}.
 */
public abstract class FulfillmentValidationException extends RuntimeException {
  protected FulfillmentValidationException(String message) {
    super(message);
  }
}
