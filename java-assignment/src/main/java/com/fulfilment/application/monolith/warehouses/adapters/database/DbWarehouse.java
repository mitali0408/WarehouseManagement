package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse")
@Cacheable
public class DbWarehouse {

  @Id @GeneratedValue public Long id;

  public String businessUnitCode;

  public String location;

  public Integer capacity;

  public Integer stock;

  public LocalDateTime createdAt;

  public LocalDateTime archivedAt;

  /**
   * Database-level safety net against the check-then-act race on businessUnitCode uniqueness:
   * the application layer (CreateWarehouseUseCase) already checks "is this code already active"
   * before creating, but that check-then-create isn't atomic, so two concurrent requests could
   * both pass the check. This column mirrors businessUnitCode while the row is active, and is
   * set to null once archived. A plain unique constraint on businessUnitCode itself would be
   * wrong — Replace deliberately keeps multiple historical rows with the same code — but most
   * databases (including Postgres) treat multiple NULLs as non-conflicting under a UNIQUE
   * constraint, so a full unique constraint on *this* column enforces "at most one active row per
   * code" without blocking the history that Replace depends on.
   */
  @Column(unique = true)
  public String activeBusinessUnitCode;

  public DbWarehouse() {}

  public static DbWarehouse fromWarehouse(Warehouse warehouse) {
    var entity = new DbWarehouse();
    entity.businessUnitCode = warehouse.businessUnitCode;
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt;
    entity.archivedAt = warehouse.archivedAt;
    entity.activeBusinessUnitCode = warehouse.archivedAt == null ? warehouse.businessUnitCode : null;
    return entity;
  }

  public void updateFrom(Warehouse warehouse) {
    this.location = warehouse.location;
    this.capacity = warehouse.capacity;
    this.stock = warehouse.stock;
    this.archivedAt = warehouse.archivedAt;
    this.activeBusinessUnitCode = warehouse.archivedAt == null ? warehouse.businessUnitCode : null;
  }

  public Warehouse toWarehouse() {
    var warehouse = new Warehouse();
    warehouse.id = this.id;
    warehouse.businessUnitCode = this.businessUnitCode;
    warehouse.location = this.location;
    warehouse.capacity = this.capacity;
    warehouse.stock = this.stock;
    warehouse.createdAt = this.createdAt;
    warehouse.archivedAt = this.archivedAt;
    return warehouse;
  }
}