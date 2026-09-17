package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Thrown when a Business Unit Code does not correspond to any currently active Warehouse.
 *
 * <p>Note this deliberately only concerns *active* warehouses: a Business Unit Code may still
 * exist in history against an archived Warehouse (see the "replace" operation), but that does not
 * count as a resolvable warehouse for operations like archive, replace, or lookup-by-id.
 */
public class WarehouseNotFoundException extends RuntimeException {

  public WarehouseNotFoundException(String businessUnitCode) {
    super("No active Warehouse found for business unit code '" + businessUnitCode + "'.");
  }
}
