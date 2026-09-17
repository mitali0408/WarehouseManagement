package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.InsufficientWarehouseCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseStockMismatchException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReplaceWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;
  @Mock LocationResolver locationResolver;

  private ReplaceWarehouseUseCase useCase() {
    return new ReplaceWarehouseUseCase(
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
  public void testReplaceSucceedsWhenValid() {
    var previous = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
    var incoming = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
    var location = new Location("ZWOLLE-001", 1, 40);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
    // the outgoing warehouse is still active (not yet archived) at the point feasibility is
    // checked — a real getByLocation call would include it, so the mock does too, genuinely
    // exercising the exclude-by-code filter rather than trivially passing on an empty list
    when(warehouseStore.getByLocation("ZWOLLE-001")).thenReturn(List.of(previous));

    useCase().replace(incoming);

    verify(warehouseStore).update(previous);
    assertNotNull(previous.archivedAt);
    verify(warehouseStore).create(incoming);
    assertNotNull(incoming.createdAt);
  }

  @Test
  public void testReplaceSucceedsAtSameLocationWhenAlreadyAtMaxCount() {
    // location allows only 1 warehouse, and the outgoing warehouse IS that one — replacing it
    // in place must succeed, since it's a swap, not a net addition. This only works if the
    // outgoing warehouse is correctly excluded from the "currently active" count before the
    // replacement is created.
    var previous = newWarehouse("MWH.001", "VETSBY-001", 40, 10);
    var incoming = newWarehouse("MWH.001", "VETSBY-001", 50, 10);
    var location = new Location("VETSBY-001", 1, 90);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("VETSBY-001")).thenReturn(location);
    when(warehouseStore.getByLocation("VETSBY-001")).thenReturn(List.of(previous));

    useCase().replace(incoming);

    verify(warehouseStore).update(previous);
    verify(warehouseStore).create(incoming);
  }


  @Test
  public void testReplaceThrowsWhenBusinessUnitCodeNotFound() {
    var incoming = newWarehouse("MWH.999", "ZWOLLE-001", 40, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);

    assertThrows(WarehouseNotFoundException.class, () -> useCase().replace(incoming));
    verify(warehouseStore, never()).update(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testReplaceThrowsWhenLocationInvalid() {
    var previous = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
    var incoming = newWarehouse("MWH.001", "NOWHERE", 40, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("NOWHERE"))
        .thenThrow(new LocationNotFoundException("NOWHERE"));

    assertThrows(LocationNotFoundException.class, () -> useCase().replace(incoming));
    verify(warehouseStore, never()).update(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testReplaceThrowsWhenStockMismatch() {
    var previous = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
    var incoming = newWarehouse("MWH.001", "ZWOLLE-001", 40, 15);
    var location = new Location("ZWOLLE-001", 1, 40);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);

    assertThrows(WarehouseStockMismatchException.class, () -> useCase().replace(incoming));
    verify(warehouseStore, never()).update(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testReplaceThrowsWhenCapacityCannotAccommodateOldStock() {
    var previous = newWarehouse("MWH.001", "ZWOLLE-001", 40, 30);
    var incoming = newWarehouse("MWH.001", "ZWOLLE-001", 20, 30);
    var location = new Location("ZWOLLE-001", 1, 40);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);

    assertThrows(
        InsufficientWarehouseCapacityException.class, () -> useCase().replace(incoming));
    verify(warehouseStore, never()).update(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testReplaceThrowsWhenNewLocationOverCapacityAfterArchive() {
    var previous = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
    // moving to a different, already-full location
    var incoming = newWarehouse("MWH.001", "TILBURG-001", 40, 10);
    var location = new Location("TILBURG-001", 2, 40);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("TILBURG-001")).thenReturn(location);
    when(warehouseStore.getByLocation("TILBURG-001"))
        .thenReturn(List.of(newWarehouse("MWH.023", "TILBURG-001", 30, 27)));

    // 30 (existing) + 40 (candidate) = 70 > maxCapacity of 40
    assertThrows(
        LocationCapacityExceededException.class, () -> useCase().replace(incoming));

    // feasibility is now validated before any mutation — a failed validation must leave nothing
    // touched, so the outgoing warehouse must NOT have been archived
    verify(warehouseStore, never()).update(any());
    verify(warehouseStore, never()).create(any());
  }
}
