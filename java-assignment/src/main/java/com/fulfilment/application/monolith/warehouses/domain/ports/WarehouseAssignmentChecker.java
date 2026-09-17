package com.fulfilment.application.monolith.warehouses.domain.ports;

/**
 * Lets the warehouses domain ask "does anything still depend on this warehouse" without knowing
 * anything about what that "anything" is. The fulfillment feature is the current (and only)
 * implementation, but the warehouses module has no dependency on it — the dependency runs the
 * other way, which is what lets a foundational module like this one stay foundational.
 */
public interface WarehouseAssignmentChecker {
  boolean hasActiveAssignments(String businessUnitCode);
}
