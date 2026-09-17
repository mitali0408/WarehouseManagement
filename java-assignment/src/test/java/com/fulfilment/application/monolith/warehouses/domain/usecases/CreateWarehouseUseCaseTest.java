package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.BusinessUnitCodeAlreadyExistsException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InsufficientWarehouseCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidWarehouseDataException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.MaxWarehousesPerLocationExceededException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;
  @Mock LocationResolver locationResolver;

  private CreateWarehouseUseCase useCase() {
    return new CreateWarehouseUseCase(
        warehouseStore, locationResolver, new WarehouseFeasibilityValidator(warehouseStore));
  }

  private static Warehouse newWarehouse(String code, String location, int capacity, int stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  @Test
  public void testCreateSucceedsWhenValid() {
    var warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 30, 10);
    var location = new Location("ZWOLLE-001", 2, 40);

    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
    when(warehouseStore.getByLocation("ZWOLLE-001")).thenReturn(List.of());

    useCase().create(warehouse);

    verify(warehouseStore).create(warehouse);
    assertNotNull(warehouse.createdAt);
  }

  @Test
  public void testCreateThrowsWhenBusinessUnitCodeAlreadyExists() {
    var warehouse = newWarehouse("MWH.001", "ZWOLLE-001", 30, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(newWarehouse("MWH.001", "X", 1, 1));

    assertThrows(BusinessUnitCodeAlreadyExistsException.class, () -> useCase().create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateThrowsWhenLocationInvalid() {
    var warehouse = newWarehouse("MWH.100", "NOWHERE", 30, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("NOWHERE"))
        .thenThrow(new LocationNotFoundException("NOWHERE"));

    assertThrows(LocationNotFoundException.class, () -> useCase().create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateThrowsWhenMaxWarehousesReachedAtLocation() {
    var warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 10, 5);
    var location = new Location("ZWOLLE-001", 1, 100);

    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
    when(warehouseStore.getByLocation("ZWOLLE-001"))
        .thenReturn(List.of(newWarehouse("MWH.001", "ZWOLLE-001", 40, 10)));

    assertThrows(
        MaxWarehousesPerLocationExceededException.class, () -> useCase().create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateSucceedsAtExactlyOneBelowMaxWarehousesForLocation() {
    // boundary case: location allows 3, exactly 2 already exist — the 3rd (this one) must succeed
    var warehouse = newWarehouse("MWH.100", "AMSTERDAM-002", 10, 5);
    var location = new Location("AMSTERDAM-002", 3, 100);

    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-002")).thenReturn(location);
    when(warehouseStore.getByLocation("AMSTERDAM-002"))
        .thenReturn(
            List.of(
                newWarehouse("MWH.001", "AMSTERDAM-002", 20, 5),
                newWarehouse("MWH.002", "AMSTERDAM-002", 20, 5)));

    useCase().create(warehouse);

    verify(warehouseStore).create(warehouse);
  }

  @Test
  public void testCreateThrowsWhenCombinedCapacityExceedsLocationMax() {
    var warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 20, 5);
    var location = new Location("ZWOLLE-001", 5, 40);

    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
    when(warehouseStore.getByLocation("ZWOLLE-001"))
        .thenReturn(List.of(newWarehouse("MWH.001", "ZWOLLE-001", 30, 10)));

    // 30 (existing) + 20 (candidate) = 50 > maxCapacity of 40
    assertThrows(LocationCapacityExceededException.class, () -> useCase().create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateThrowsWhenStockExceedsOwnCapacity() {
    var warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 10, 20);
    var location = new Location("ZWOLLE-001", 5, 100);

    when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
    when(warehouseStore.getByLocation("ZWOLLE-001")).thenReturn(List.of());

    assertThrows(
        InsufficientWarehouseCapacityException.class, () -> useCase().create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateThrowsWhenRequiredFieldMissing() {
    var warehouse = newWarehouse(null, "ZWOLLE-001", 10, 5);

    assertThrows(InvalidWarehouseDataException.class, () -> useCase().create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateThrowsWhenLocationMissing() {
    var warehouse = newWarehouse("TEST.NO.LOC", null, 10, 5);

    assertThrows(InvalidWarehouseDataException.class, () -> useCase().create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @ParameterizedTest
  @MethodSource("invalidCapacities")
  public void testCreateThrowsForInvalidCapacity(Integer invalidCapacity) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "TEST.INVALID.CAP";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = invalidCapacity;
    warehouse.stock = 0;

    assertThrows(InvalidWarehouseDataException.class, () -> useCase().create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  private static Stream<Integer> invalidCapacities() {
    // capacity must be strictly positive: null, negative, and zero are all invalid
    return Stream.of(null, -1, 0);
  }

  @ParameterizedTest
  @MethodSource("invalidStocks")
  public void testCreateThrowsForInvalidStock(Integer invalidStock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "TEST.INVALID.STOCK";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 10;
    warehouse.stock = invalidStock;

    assertThrows(InvalidWarehouseDataException.class, () -> useCase().create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  private static Stream<Integer> invalidStocks() {
    // stock must be non-negative: null and negative are invalid, but zero is a valid (empty)
    // stock, so it's deliberately NOT in this list
    return Stream.of(null, -1);
  }
}
