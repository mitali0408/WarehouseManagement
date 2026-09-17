package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Packaged-artifact smoke test: runs against the built jar (not instrumented by JaCoCo — see the
 * in-process {@link WarehouseEndpointTest} for coverage-contributing tests of the same
 * endpoints). Kept intentionally small: this is a deployment sanity check, not the primary test
 * suite for this resource.
 */
@QuarkusIntegrationTest
public class WarehouseEndpointIT {

  @Test
  public void testSimpleListWarehouses() {
    final String path = "warehouse";

    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void testSimpleCheckingArchivingWarehouses() {
    final String path = "warehouse";

    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));

    // Archive the ZWOLLE-001 warehouse (MWH.001, seeded with database id 1):
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, MWH.001 should be missing now (archived warehouses are excluded from listing):
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            not(containsString("MWH.001")),
            containsString("MWH.012"),
            containsString("MWH.023"));
  }
}
