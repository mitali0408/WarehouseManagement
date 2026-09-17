package com.fulfilment.application.monolith.fulfillment;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class FulfillmentEntityNotFoundExceptionMapper
    implements ExceptionMapper<FulfillmentEntityNotFoundException> {

  @Override
  public Response toResponse(FulfillmentEntityNotFoundException exception) {
    return Response.status(Response.Status.NOT_FOUND)
        .type(MediaType.APPLICATION_JSON)
        .entity(new ErrorBody(exception.getMessage()))
        .build();
  }

  private record ErrorBody(String error) {}
}
