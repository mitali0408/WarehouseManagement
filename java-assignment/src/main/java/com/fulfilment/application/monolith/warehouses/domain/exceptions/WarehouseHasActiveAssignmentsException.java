package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Thrown when attempting to archive a Warehouse that still has active fulfillment assignments
 * (Product/Store links) pointing at it. Archiving anyway would leave those assignments dangling
 * — referencing a business unit code with no active warehouse behind it — so the caller must
 * remove them first.
 */
public class WarehouseHasActiveAssignmentsException extends InvalidWarehouseOperationException {
  public WarehouseHasActiveAssignmentsException(String businessUnitCode) {
    super(
        "Warehouse '"
            + businessUnitCode
            + "' cannot be archived: it still has active fulfillment assignments. Remove those"
            + " first.");
  }
}
