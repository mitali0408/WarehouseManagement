package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidWarehouseOperationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Maps every business-rule violation raised by the Warehouse domain (invalid location, business
 * unit code conflicts, capacity/stock violations, etc.) to HTTP 400, matching the "Invalid
 * request parameters" response documented in warehouse-openapi.yaml.
 */
@Provider
public class InvalidWarehouseOperationExceptionMapper
    implements ExceptionMapper<InvalidWarehouseOperationException> {

  private static final Logger LOGGER =
      Logger.getLogger(InvalidWarehouseOperationExceptionMapper.class);

  @Override
  public Response toResponse(InvalidWarehouseOperationException exception) {
    LOGGER.warn("Rejected invalid warehouse operation: " + exception.getMessage());
    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON)
        .entity(new ErrorBody(exception.getMessage()))
        .build();
  }

  private record ErrorBody(String error) {}
}
