package com.fulfilment.application.monolith.fulfillment;

public class StoreNotFoundException extends FulfillmentEntityNotFoundException {
  public StoreNotFoundException(Long storeId) {
    super("Store " + storeId + " does not exist.");
  }
}
