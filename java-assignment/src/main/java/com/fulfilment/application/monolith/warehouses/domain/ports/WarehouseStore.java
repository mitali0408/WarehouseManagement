package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

public interface WarehouseStore {

  List<Warehouse> getAll();

  List<Warehouse> getByLocation(String location);

  void create(Warehouse warehouse);

  void update(Warehouse warehouse);

  void remove(Warehouse warehouse);

  Warehouse findByBusinessUnitCode(String buCode);
}
