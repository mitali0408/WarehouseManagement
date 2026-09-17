package com.fulfilment.application.monolith.fulfillment;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class FulfillmentValidationExceptionMapper
    implements ExceptionMapper<FulfillmentValidationException> {

  private static final Logger LOGGER = Logger.getLogger(FulfillmentValidationExceptionMapper.class);

  @Override
  public Response toResponse(FulfillmentValidationException exception) {
    LOGGER.warn("Rejected invalid fulfillment assignment: " + exception.getMessage());
    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON)
        .entity(new ErrorBody(exception.getMessage()))
        .build();
  }

  private record ErrorBody(String error) {}
}
