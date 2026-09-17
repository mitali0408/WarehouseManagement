package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

public class WarehouseNotFoundExceptionMapperTest {

    @Test
    public void testToResponse_returns404WithMessage() {
        var mapper = new WarehouseNotFoundExceptionMapper();
        var exception = new WarehouseNotFoundException("MWH.999");

        Response response = mapper.toResponse(exception);

        assertEquals(404, response.getStatus());
        assertNotNull(response.getEntity());
        assertTrue(response.getEntity().toString().contains("MWH.999"));
    }
}