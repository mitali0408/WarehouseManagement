package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.BusinessUnitCodeAlreadyExistsException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

public class InvalidWarehouseOperationExceptionMapperTest {

    @Test
    public void testToResponse_returns400WithMessage() {
        var mapper = new InvalidWarehouseOperationExceptionMapper();
        var exception = new BusinessUnitCodeAlreadyExistsException("MWH.001");

        Response response = mapper.toResponse(exception);

        assertEquals(400, response.getStatus());
        assertNotNull(response.getEntity());
        assertTrue(response.getEntity().toString().contains("MWH.001"));
    }
}