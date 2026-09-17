package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Records that a Warehouse (identified by its business unit code) is a fulfillment unit for a
 * given Product at a given Store. The unique constraint prevents the exact same triple being
 * recorded twice; the three quantity constraints on top of this (max warehouses per
 * product-per-store, max warehouses per store, max product types per warehouse) are business
 * rules enforced in {@link AssignWarehouseToProductInStoreUseCase}, not at the schema level.
 */
@Entity
@Table(
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"productId", "storeId", "warehouseBusinessUnitCode"}))
public class WarehouseProductStoreAssociation extends PanacheEntity {

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = false)
  public Long storeId;

  @Column(nullable = false, length = 40)
  public String warehouseBusinessUnitCode;

  public WarehouseProductStoreAssociation() {}

  public WarehouseProductStoreAssociation(
      Long productId, Long storeId, String warehouseBusinessUnitCode) {
    this.productId = productId;
    this.storeId = storeId;
    this.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
  }
}
