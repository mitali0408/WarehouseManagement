package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

public class FulfillmentValidationExceptionMapperTest {

    @Test
    public void testToResponse_returns400WithMessage() {
        var mapper = new FulfillmentValidationExceptionMapper();
        var exception = new DuplicateFulfillmentAssignmentException(1L, 1L, "MWH.001");

        Response response = mapper.toResponse(exception);

        assertEquals(400, response.getStatus());
        assertNotNull(response.getEntity());
        assertTrue(response.getEntity().toString().contains("MWH.001"));
    }
}