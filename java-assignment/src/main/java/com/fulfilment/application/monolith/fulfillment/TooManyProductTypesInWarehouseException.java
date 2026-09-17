package com.fulfilment.application.monolith.fulfillment;

public class TooManyProductTypesInWarehouseException extends FulfillmentValidationException {
  public TooManyProductTypesInWarehouseException(String warehouseBusinessUnitCode, int max) {
    super(
        "Warehouse '"
            + warehouseBusinessUnitCode
            + "' already stocks the maximum of "
            + max
            + " different product type(s).");
  }
}
