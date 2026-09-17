package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Thrown when a Location identifier does not correspond to any known, valid location.
 *
 * <p>Kept as an unchecked domain exception so it can propagate cleanly through the use-case layer
 * without polluting method signatures; adapters (e.g. REST resources) are responsible for mapping
 * it to an appropriate transport-level response (e.g. HTTP 400).
 */
public class LocationNotFoundException extends InvalidWarehouseOperationException {

  public LocationNotFoundException(String identifier) {
    super("Location with identifier '" + identifier + "' does not exist.");
  }
}
