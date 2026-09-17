package com.fulfilment.application.monolith.fulfillment;

/**
 * Query/persistence contract the assignment use case needs, kept small and specific to the
 * exact questions the three business rules ask — rather than a generic CRUD repository — so the
 * use case can be unit tested with a mock.
 */
public interface FulfillmentAssignmentStore {

  boolean existsExact(Long productId, Long storeId, String warehouseBusinessUnitCode);

  long countDistinctWarehousesForProductInStore(Long productId, Long storeId);

  boolean warehouseAlreadyServesStore(Long storeId, String warehouseBusinessUnitCode);

  long countDistinctWarehousesForStore(Long storeId);

  boolean warehouseAlreadyHandlesProduct(String warehouseBusinessUnitCode, Long productId);

  long countDistinctProductsForWarehouse(String warehouseBusinessUnitCode);

  void save(WarehouseProductStoreAssociation association);
}
