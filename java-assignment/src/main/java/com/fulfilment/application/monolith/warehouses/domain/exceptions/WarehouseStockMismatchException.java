package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Thrown when replacing a Warehouse and the new Warehouse's stock does not match the stock of
 * the Warehouse being replaced, as required by the replace operation.
 */
public class WarehouseStockMismatchException extends InvalidWarehouseOperationException {

  public WarehouseStockMismatchException(
      String businessUnitCode, Integer previousStock, Integer newStock) {
    super(
        "Cannot replace warehouse '"
            + businessUnitCode
            + "': new stock ("
            + newStock
            + ") must match the stock of the warehouse being replaced ("
            + previousStock
            + ").");
  }
}
