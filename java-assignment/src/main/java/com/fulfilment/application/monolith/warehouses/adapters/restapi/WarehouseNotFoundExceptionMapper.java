package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Maps a WarehouseNotFoundException to HTTP 404, matching the "Warehouse unit not found"
 * response documented in warehouse-openapi.yaml.
 */
@Provider
public class WarehouseNotFoundExceptionMapper
    implements ExceptionMapper<WarehouseNotFoundException> {

  private static final Logger LOGGER = Logger.getLogger(WarehouseNotFoundExceptionMapper.class);

  @Override
  public Response toResponse(WarehouseNotFoundException exception) {
    LOGGER.warn(exception.getMessage());
    return Response.status(Response.Status.NOT_FOUND)
        .type(MediaType.APPLICATION_JSON)
        .entity(new ErrorBody(exception.getMessage()))
        .build();
  }

  private record ErrorBody(String error) {}
}
