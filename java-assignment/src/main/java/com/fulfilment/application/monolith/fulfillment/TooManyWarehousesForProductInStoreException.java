package com.fulfilment.application.monolith.fulfillment;

public class TooManyWarehousesForProductInStoreException extends FulfillmentValidationException {
  public TooManyWarehousesForProductInStoreException(Long productId, Long storeId, int max) {
    super(
        "Product "
            + productId
            + " at store "
            + storeId
            + " is already fulfilled by the maximum of "
            + max
            + " warehouse(s).");
  }
}
