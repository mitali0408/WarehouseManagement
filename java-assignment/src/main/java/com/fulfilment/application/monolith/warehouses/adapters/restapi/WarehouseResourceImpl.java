package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import io.vertx.core.http.HttpServerResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;

  @Context private HttpServerResponse vertxResponse;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  @Transactional
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var warehouse = toDomainWarehouse(data);

    createWarehouseOperation.create(warehouse);

    // The generated interface fixes this method's return type as Warehouse (not Response), so
    // the 201 status warehouse-openapi.yaml documents for a successful creation is set directly
    // on the underlying Vert.x response here rather than via a JAX-RS Response wrapper.
    vertxResponse.setStatusCode(201);

    return toWarehouseResponse(warehouse);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    DbWarehouse entity = findEntityByDatabaseId(id);
    if (entity.archivedAt != null) {
      throw new WebApplicationException("Warehouse with id " + id + " does not exist.", 404);
    }
    return toWarehouseResponse(entity.toWarehouse());
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    DbWarehouse entity = findEntityByDatabaseId(id);

    if (entity.archivedAt != null) {
      throw new WebApplicationException(
          "Warehouse with id " + id + " is already archived.", 404);
    }

    archiveWarehouseOperation.archive(entity.toWarehouse());
  }

  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    var warehouse = toDomainWarehouse(data);
    // the path parameter is the authoritative business unit code being replaced —
    // whatever (if anything) is in the request body for this field is ignored.
    warehouse.businessUnitCode = businessUnitCode;

    replaceWarehouseOperation.replace(warehouse);

    return toWarehouseResponse(warehouse);
  }

  private DbWarehouse findEntityByDatabaseId(String id) {
    Long numericId;
    try {
      numericId = Long.valueOf(id);
    } catch (NumberFormatException e) {
      throw new WebApplicationException("Invalid warehouse id: '" + id + "'.", 400);
    }

    DbWarehouse entity = warehouseRepository.findById(numericId);
    if (entity == null) {
      throw new WebApplicationException("Warehouse with id " + id + " does not exist.", 404);
    }
    return entity;
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse
      toDomainWarehouse(Warehouse data) {
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();
    return warehouse;
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    if (warehouse.id != null) {
      response.setId(String.valueOf(warehouse.id));
    }
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }
}
