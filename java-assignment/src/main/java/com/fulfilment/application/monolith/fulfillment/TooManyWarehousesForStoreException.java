package com.fulfilment.application.monolith.fulfillment;

public class TooManyWarehousesForStoreException extends FulfillmentValidationException {
  public TooManyWarehousesForStoreException(Long storeId, int max) {
    super(
        "Store "
            + storeId
            + " is already fulfilled by the maximum of "
            + max
            + " different warehouse(s).");
  }
}
