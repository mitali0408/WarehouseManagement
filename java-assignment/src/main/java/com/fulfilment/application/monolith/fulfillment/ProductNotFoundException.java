package com.fulfilment.application.monolith.fulfillment;

public class ProductNotFoundException extends FulfillmentEntityNotFoundException {
  public ProductNotFoundException(Long productId) {
    super("Product " + productId + " does not exist.");
  }
}
