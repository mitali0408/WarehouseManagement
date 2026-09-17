package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.BusinessUnitCodeAlreadyExistsException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.list("archivedAt is null").stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public List<Warehouse> getByLocation(String location) {
    return this.list("location = ?1 and archivedAt is null", location).stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse entity = DbWarehouse.fromWarehouse(warehouse);
    try {
      persist(entity);
      // Force the INSERT (and therefore the activeBusinessUnitCode unique constraint check) to
      // happen now, inside this try block, rather than at some later implicit flush point where
      // it would be harder to attribute a failure specifically to this create() call.
      getEntityManager().flush();
    } catch (PersistenceException e) {
      // The application-level check in CreateWarehouseUseCase (findByBusinessUnitCode before
      // create) is the primary defense against duplicate active codes; this is the safety net
      // for the race window between that check and this write. In practice this entity has only
      // one unique constraint that a normal create() could violate, so treating any
      // PersistenceException here as that specific conflict is a reasonable simplification.
      throw new BusinessUnitCodeAlreadyExistsException(warehouse.businessUnitCode);
    }
    // the DB-generated id lives on the new entity; copy it back onto the domain object so
    // callers (e.g. the REST layer, to include "id" in a create response) can see it too.
    warehouse.id = entity.id;
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse entity = findActiveEntityByBusinessUnitCode(warehouse.businessUnitCode);
    entity.updateFrom(warehouse);
    // Explicit flush, for the same reason create() has one: Hibernate's default flush ordering
    // runs pending INSERTs before pending UPDATEs within a single flush. Replace calls update()
    // (archiving the outgoing warehouse, clearing its activeBusinessUnitCode) immediately
    // followed by create() (the replacement, with the SAME businessUnitCode). Without forcing
    // this update to flush first, both rows would still hold that value at the moment the new
    // INSERT runs, tripping the unique constraint on a legitimate replace.
    getEntityManager().flush();
  }

  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse entity = findActiveEntityByBusinessUnitCode(warehouse.businessUnitCode);
    delete(entity);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse entity = find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
    return entity == null ? null : entity.toWarehouse();
  }

  private DbWarehouse findActiveEntityByBusinessUnitCode(String buCode) {
    DbWarehouse entity = find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
    if (entity == null) {
      throw new WarehouseNotFoundException(buCode);
    }
    return entity;
  }
}
