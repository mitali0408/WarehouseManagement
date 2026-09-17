package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AssignWarehouseToProductInStoreUseCase {

  static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  static final int MAX_WAREHOUSES_PER_STORE = 3;
  static final int MAX_PRODUCT_TYPES_PER_WAREHOUSE = 5;

  private final FulfillmentAssignmentStore assignmentStore;
  private final WarehouseStore warehouseStore;
  private final ProductRepository productRepository;
  private final StoreRepository storeRepository;

  public AssignWarehouseToProductInStoreUseCase(
          FulfillmentAssignmentStore assignmentStore,
          WarehouseStore warehouseStore,
          ProductRepository productRepository,
          StoreRepository storeRepository) {
    this.assignmentStore = assignmentStore;
    this.warehouseStore = warehouseStore;
    this.productRepository = productRepository;
    this.storeRepository = storeRepository;
  }

  public WarehouseProductStoreAssociation assign(
          Long productId, Long storeId, String warehouseBusinessUnitCode) {
    if (productRepository.findById(productId) == null) {
      throw new ProductNotFoundException(productId);
    }
    if (storeRepository.findById(storeId) == null) {
      throw new StoreNotFoundException(storeId);
    }
    if (warehouseStore.findByBusinessUnitCode(warehouseBusinessUnitCode) == null) {
      throw new WarehouseNotFoundException(warehouseBusinessUnitCode);
    }

    if (assignmentStore.existsExact(productId, storeId, warehouseBusinessUnitCode)) {
      throw new DuplicateFulfillmentAssignmentException(
              productId, storeId, warehouseBusinessUnitCode);
    }

    // Constraint 1: a product can be fulfilled by at most 2 different warehouses per store.
    if (assignmentStore.countDistinctWarehousesForProductInStore(productId, storeId)
            >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new TooManyWarehousesForProductInStoreException(
              productId, storeId, MAX_WAREHOUSES_PER_PRODUCT_PER_STORE);
    }

    // Constraint 2: a store can be fulfilled by at most 3 different warehouses (across all its
    // products). Only relevant if this warehouse is *new* to this store — assigning it to
    // another product it already serves at this store doesn't add to the store's warehouse count.
    boolean warehouseAlreadyAtStore =
            assignmentStore.warehouseAlreadyServesStore(storeId, warehouseBusinessUnitCode);
    if (!warehouseAlreadyAtStore
            && assignmentStore.countDistinctWarehousesForStore(storeId) >= MAX_WAREHOUSES_PER_STORE) {
      throw new TooManyWarehousesForStoreException(storeId, MAX_WAREHOUSES_PER_STORE);
    }

    // Constraint 3: a warehouse can stock at most 5 different product types (across all stores).
    // Same "only if new" logic, mirrored for the warehouse's side of the relationship.
    boolean warehouseAlreadyHandlesProduct =
            assignmentStore.warehouseAlreadyHandlesProduct(warehouseBusinessUnitCode, productId);
    if (!warehouseAlreadyHandlesProduct
            && assignmentStore.countDistinctProductsForWarehouse(warehouseBusinessUnitCode)
            >= MAX_PRODUCT_TYPES_PER_WAREHOUSE) {
      throw new TooManyProductTypesInWarehouseException(
              warehouseBusinessUnitCode, MAX_PRODUCT_TYPES_PER_WAREHOUSE);
    }

    var association =
            new WarehouseProductStoreAssociation(productId, storeId, warehouseBusinessUnitCode);
    assignmentStore.save(association);
    return association;
  }
}