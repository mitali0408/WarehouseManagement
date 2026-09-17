package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  private final LocationGateway locationGateway = new LocationGateway();

  @Test
  public void testWhenResolveExistingLocationShouldReturn() {
    // when
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    // then
    assertEquals("ZWOLLE-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  public void testWhenResolveAnotherExistingLocationShouldReturnCorrectOne() {
    // when
    Location location = locationGateway.resolveByIdentifier("AMSTERDAM-001");

    // then
    assertEquals("AMSTERDAM-001", location.identification);
    assertEquals(5, location.maxNumberOfWarehouses);
    assertEquals(100, location.maxCapacity);
  }

  @Test
  public void testWhenResolveNonExistingLocationShouldThrow() {
    // when / then
    assertThrows(
        LocationNotFoundException.class,
        () -> locationGateway.resolveByIdentifier("NONEXISTENT-999"));
  }
}
