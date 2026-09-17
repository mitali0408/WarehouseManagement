package com.fulfilment.application.monolith.fulfillment;

/**
 * Base type for "the Product/Store referenced in a fulfillment assignment doesn't exist"
 * failures. Mapped centrally to HTTP 404 by {@link FulfillmentEntityNotFoundExceptionMapper}.
 */
public abstract class FulfillmentEntityNotFoundException extends RuntimeException {
  protected FulfillmentEntityNotFoundException(String message) {
    super(message);
  }
}
