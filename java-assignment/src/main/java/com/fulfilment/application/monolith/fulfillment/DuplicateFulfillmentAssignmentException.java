package com.fulfilment.application.monolith.fulfillment;

public class DuplicateFulfillmentAssignmentException extends FulfillmentValidationException {
  public DuplicateFulfillmentAssignmentException(
      Long productId, Long storeId, String warehouseBusinessUnitCode) {
    super(
        "Warehouse '"
            + warehouseBusinessUnitCode
            + "' is already assigned to fulfill product "
            + productId
            + " for store "
            + storeId
            + ".");
  }
}
