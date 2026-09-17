package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

public class FulfillmentEntityNotFoundExceptionMapperTest {

    @Test
    public void testToResponse_returns404WithMessage() {
        var mapper = new FulfillmentEntityNotFoundExceptionMapper();
        var exception = new ProductNotFoundException(999L);

        Response response = mapper.toResponse(exception);

        assertEquals(404, response.getStatus());
        assertNotNull(response.getEntity());
        assertTrue(response.getEntity().toString().contains("999"));
    }
}