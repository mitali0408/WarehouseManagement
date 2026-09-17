package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FulfillmentAssignmentRepository
    implements FulfillmentAssignmentStore, PanacheRepository<WarehouseProductStoreAssociation> {

  @Override
  public boolean existsExact(Long productId, Long storeId, String warehouseBusinessUnitCode) {
    return count(
            "productId = ?1 and storeId = ?2 and warehouseBusinessUnitCode = ?3",
            productId,
            storeId,
            warehouseBusinessUnitCode)
        > 0;
  }

  @Override
  public long countDistinctWarehousesForProductInStore(Long productId, Long storeId) {
    return getEntityManager()
        .createQuery(
            "select count(distinct a.warehouseBusinessUnitCode) "
                + "from WarehouseProductStoreAssociation a "
                + "where a.productId = :productId and a.storeId = :storeId",
            Long.class)
        .setParameter("productId", productId)
        .setParameter("storeId", storeId)
        .getSingleResult();
  }

  @Override
  public boolean warehouseAlreadyServesStore(Long storeId, String warehouseBusinessUnitCode) {
    return count("storeId = ?1 and warehouseBusinessUnitCode = ?2", storeId, warehouseBusinessUnitCode)
        > 0;
  }

  @Override
  public long countDistinctWarehousesForStore(Long storeId) {
    return getEntityManager()
        .createQuery(
            "select count(distinct a.warehouseBusinessUnitCode) "
                + "from WarehouseProductStoreAssociation a "
                + "where a.storeId = :storeId",
            Long.class)
        .setParameter("storeId", storeId)
        .getSingleResult();
  }

  @Override
  public boolean warehouseAlreadyHandlesProduct(String warehouseBusinessUnitCode, Long productId) {
    return count("warehouseBusinessUnitCode = ?1 and productId = ?2", warehouseBusinessUnitCode, productId)
        > 0;
  }

  @Override
  public long countDistinctProductsForWarehouse(String warehouseBusinessUnitCode) {
    return getEntityManager()
        .createQuery(
            "select count(distinct a.productId) "
                + "from WarehouseProductStoreAssociation a "
                + "where a.warehouseBusinessUnitCode = :code",
            Long.class)
        .setParameter("code", warehouseBusinessUnitCode)
        .getSingleResult();
  }

  @Override
  public void save(WarehouseProductStoreAssociation association) {
    persist(association);
  }
}
