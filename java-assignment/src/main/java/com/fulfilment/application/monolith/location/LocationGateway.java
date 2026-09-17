package com.fulfilment.application.monolith.location;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class LocationGateway implements LocationResolver {

  // LinkedHashMap keyed by identification for O(1) lookup, rather than a linear scan over a
  // list; insertion order preserved purely for readability (no functional dependency on it).
  private static final Map<String, Location> locationsByIdentification = new LinkedHashMap<>();

  static {
    addLocation(new Location("ZWOLLE-001", 1, 40));
    addLocation(new Location("ZWOLLE-002", 2, 50));
    addLocation(new Location("AMSTERDAM-001", 5, 100));
    addLocation(new Location("AMSTERDAM-002", 3, 75));
    addLocation(new Location("TILBURG-001", 1, 40));
    addLocation(new Location("HELMOND-001", 1, 45));
    addLocation(new Location("EINDHOVEN-001", 2, 70));
    addLocation(new Location("VETSBY-001", 1, 90));
  }

  private static void addLocation(Location location) {
    locationsByIdentification.put(location.identification, location);
  }

  @Override
  public Location resolveByIdentifier(String identifier) {
    Location location = locationsByIdentification.get(identifier);
    if (location == null) {
      throw new LocationNotFoundException(identifier);
    }
    return location;
  }
}
