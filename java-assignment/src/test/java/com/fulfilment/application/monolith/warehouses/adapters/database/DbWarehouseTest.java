package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Plain, dependency-free unit tests for DbWarehouse's field-mapping methods — no DB, no CDI, no
 * Quarkus boot needed, since these are pure POJO transformations. Previously only exercised
 * incidentally through other (slower, DB-backed) tests, never asserted on directly.
 */
public class DbWarehouseTest {

    @Test
    public void testFromWarehouse_copiesAllFieldsAndSetsActiveCodeWhenNotArchived() {
        var warehouse = new Warehouse();
        warehouse.businessUnitCode = "MWH.001";
        warehouse.location = "ZWOLLE-001";
        warehouse.capacity = 40;
        warehouse.stock = 10;
        warehouse.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        warehouse.archivedAt = null;

        DbWarehouse entity = DbWarehouse.fromWarehouse(warehouse);

        assertEquals("MWH.001", entity.businessUnitCode);
        assertEquals("ZWOLLE-001", entity.location);
        assertEquals(40, entity.capacity);
        assertEquals(10, entity.stock);
        assertEquals(warehouse.createdAt, entity.createdAt);
        assertNull(entity.archivedAt);
        // active (not archived), so the shadow column mirrors businessUnitCode
        assertEquals("MWH.001", entity.activeBusinessUnitCode);
    }

    @Test
    public void testFromWarehouse_setsActiveCodeNullWhenArchived() {
        var warehouse = new Warehouse();
        warehouse.businessUnitCode = "MWH.001";
        warehouse.location = "ZWOLLE-001";
        warehouse.capacity = 40;
        warehouse.stock = 10;
        warehouse.archivedAt = LocalDateTime.of(2026, 1, 2, 0, 0);

        DbWarehouse entity = DbWarehouse.fromWarehouse(warehouse);

        assertEquals(warehouse.archivedAt, entity.archivedAt);
        // archived, so the shadow column must be null — this is the whole point of the
        // uniqueness-safety-net design (see DbWarehouse's own Javadoc)
        assertNull(entity.activeBusinessUnitCode);
    }

    @Test
    public void testUpdateFrom_updatesMutableFieldsButNotBusinessUnitCodeOrCreatedAt() {
        var entity = new DbWarehouse();
        entity.businessUnitCode = "MWH.001";
        entity.location = "OLD-LOCATION";
        entity.capacity = 10;
        entity.stock = 5;
        entity.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        entity.activeBusinessUnitCode = "MWH.001";

        var updated = new Warehouse();
        updated.businessUnitCode = "MWH.001";
        updated.location = "NEW-LOCATION";
        updated.capacity = 20;
        updated.stock = 15;

        entity.updateFrom(updated);

        assertEquals("NEW-LOCATION", entity.location);
        assertEquals(20, entity.capacity);
        assertEquals(15, entity.stock);
        // businessUnitCode and createdAt are deliberately NOT touched by updateFrom — they're
        // identity/creation-time facts of a specific warehouse instance, not mutable on update
        assertEquals("MWH.001", entity.businessUnitCode);
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), entity.createdAt);
    }

    @Test
    public void testUpdateFrom_clearsActiveCodeWhenArchiving() {
        var entity = new DbWarehouse();
        entity.businessUnitCode = "MWH.001";
        entity.activeBusinessUnitCode = "MWH.001";

        var archived = new Warehouse();
        archived.businessUnitCode = "MWH.001";
        archived.location = "ZWOLLE-001";
        archived.capacity = 40;
        archived.stock = 10;
        archived.archivedAt = LocalDateTime.of(2026, 1, 3, 0, 0);

        entity.updateFrom(archived);

        assertEquals(archived.archivedAt, entity.archivedAt);
        assertNull(entity.activeBusinessUnitCode);
    }

    @Test
    public void testToWarehouse_copiesAllFieldsIncludingId() {
        var entity = new DbWarehouse();
        entity.id = 42L;
        entity.businessUnitCode = "MWH.001";
        entity.location = "ZWOLLE-001";
        entity.capacity = 40;
        entity.stock = 10;
        entity.createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        entity.archivedAt = LocalDateTime.of(2026, 1, 2, 0, 0);

        Warehouse warehouse = entity.toWarehouse();

        assertEquals(42L, warehouse.id);
        assertEquals("MWH.001", warehouse.businessUnitCode);
        assertEquals("ZWOLLE-001", warehouse.location);
        assertEquals(40, warehouse.capacity);
        assertEquals(10, warehouse.stock);
        assertEquals(entity.createdAt, warehouse.createdAt);
        assertEquals(entity.archivedAt, warehouse.archivedAt);
    }
}
