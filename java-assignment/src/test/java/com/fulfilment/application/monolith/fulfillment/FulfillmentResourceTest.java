package com.fulfilment.application.monolith.fulfillment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * In-process HTTP-level tests for FulfillmentResource, covering routing, request validation and
 * exception-to-status-code mapping. Business-rule edge cases themselves are already thoroughly
 * covered at the use-case level by {@link AssignWarehouseToProductInStoreUseCaseTest}; this class
 * focuses on what's specific to the REST layer.
 *
 * <p>Each test is {@code @TestTransaction}, rolling back its own writes — independent of other
 * tests and of execution order, regardless of which store/product/warehouse it happens to use.
 *
 * <p>Test products are created via the actual {@code POST /product} endpoint rather than a
 * direct repository call, deliberately — an HTTP call is dispatched on a different thread than
 * the test method's own, and mixing a direct in-thread write with a later HTTP read within one
 * {@code @TestTransaction} risks the HTTP request's own database session not seeing it. Staying
 * HTTP-to-HTTP throughout avoids that entirely.
 */
@QuarkusTest
public class FulfillmentResourceTest {

  private static Long createProduct(String name) {
    String body = String.format("{\"name\":\"%s\"}", name);
    return Long.valueOf(
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("product")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString());
  }

  @Test
  @TestTransaction
  public void testAssignAndList_success() {
    Long productId = createProduct("FULFILL-REST-TEST-P1");

    String body =
        String.format(
            "{\"productId\":%d,\"storeId\":1,\"warehouseBusinessUnitCode\":\"MWH.023\"}",
            productId);

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("fulfillment")
        .then()
        .statusCode(201)
        .header("Location", containsString("/fulfillment/"))
        .body(containsString("MWH.023"))
        .body("id", notNullValue());

    given()
        .when()
        .get("fulfillment/store/1")
        .then()
        .statusCode(200)
        .body(containsString("MWH.023"));

    given()
        .when()
        .get("fulfillment/product/" + productId)
        .then()
        .statusCode(200)
        .body(containsString("MWH.023"));
  }

  @Test
  @TestTransaction
  public void testGetById_successAndNotFound() {
    Long productId = createProduct("FULFILL-REST-TEST-GETBYID");
    String body =
        String.format(
            "{\"productId\":%d,\"storeId\":1,\"warehouseBusinessUnitCode\":\"MWH.023\"}",
            productId);

    String id =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("fulfillment")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    given()
        .when()
        .get("fulfillment/" + id)
        .then()
        .statusCode(200)
        .body(containsString("MWH.023"));

    given().when().get("fulfillment/999999").then().statusCode(404);
  }

  @Test
  @TestTransaction
  public void testGetById_nonPositiveIdReturns400() {
    given().when().get("fulfillment/-1").then().statusCode(400);
  }

  @Test
  @TestTransaction
  public void testDelete_successAndNotFound() {
    Long productId = createProduct("FULFILL-REST-TEST-DELETE");
    String body =
        String.format(
            "{\"productId\":%d,\"storeId\":1,\"warehouseBusinessUnitCode\":\"MWH.023\"}",
            productId);

    String id =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("fulfillment")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    given().when().get("fulfillment/" + id).then().statusCode(200);

    given().when().delete("fulfillment/" + id).then().statusCode(204);

    given().when().get("fulfillment/" + id).then().statusCode(404);

    // deleting an already-deleted assignment is treated as not-found
    given().when().delete("fulfillment/" + id).then().statusCode(404);
  }

  @Test
  @TestTransaction
  public void testDelete_freesUpSlotAtLimit() {
    // Removing an assignment must genuinely free up the slot it occupied — this is exactly the
    // scenario an assign-only, no-delete API would make impossible: once a limit is reached,
    // there'd be no way to make room for a different warehouse.
    Long productId = createProduct("FULFILL-REST-TEST-DELETE-LIMIT");
    String first =
        String.format(
            "{\"productId\":%d,\"storeId\":1,\"warehouseBusinessUnitCode\":\"MWH.001\"}",
            productId);
    String second =
        String.format(
            "{\"productId\":%d,\"storeId\":1,\"warehouseBusinessUnitCode\":\"MWH.012\"}",
            productId);
    String third =
        String.format(
            "{\"productId\":%d,\"storeId\":1,\"warehouseBusinessUnitCode\":\"MWH.023\"}",
            productId);

    given().contentType(ContentType.JSON).body(first).when().post("fulfillment").then().statusCode(201);
    String secondId =
        given()
            .contentType(ContentType.JSON)
            .body(second)
            .when()
            .post("fulfillment")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    // at the max of 2 warehouses for this product at this store — a 3rd is rejected
    given().contentType(ContentType.JSON).body(third).when().post("fulfillment").then().statusCode(400);

    // remove one of the two, freeing a slot
    given().when().delete("fulfillment/" + secondId).then().statusCode(204);

    // now the 3rd assignment succeeds
    given().contentType(ContentType.JSON).body(third).when().post("fulfillment").then().statusCode(201);
  }

  @Test
  @TestTransaction
  public void testDelete_nonPositiveIdReturns400() {
    given().when().delete("fulfillment/0").then().statusCode(400);
  }

  @Test
  @TestTransaction
  public void testAssign_duplicateReturns400() {
    Long productId = createProduct("FULFILL-REST-TEST-P2");

    String body =
        String.format(
            "{\"productId\":%d,\"storeId\":1,\"warehouseBusinessUnitCode\":\"MWH.012\"}",
            productId);

    given().contentType(ContentType.JSON).body(body).when().post("fulfillment").then().statusCode(201);
    given().contentType(ContentType.JSON).body(body).when().post("fulfillment").then().statusCode(400);
  }

  @Test
  @TestTransaction
  public void testAssign_missingFieldsReturns400() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"productId\":1}")
        .when()
        .post("fulfillment")
        .then()
        .statusCode(400);
  }

  @Test
  @TestTransaction
  public void testAssign_nonPositiveIdsReturn400() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"productId\":-1,\"storeId\":1,\"warehouseBusinessUnitCode\":\"MWH.001\"}")
        .when()
        .post("fulfillment")
        .then()
        .statusCode(400);

    given()
        .contentType(ContentType.JSON)
        .body("{\"productId\":1,\"storeId\":0,\"warehouseBusinessUnitCode\":\"MWH.001\"}")
        .when()
        .post("fulfillment")
        .then()
        .statusCode(400);
  }

  @Test
  @TestTransaction
  public void testListByStore_nonPositiveIdReturns400() {
    given().when().get("fulfillment/store/-1").then().statusCode(400);
  }

  @Test
  @TestTransaction
  public void testListByProduct_nonPositiveIdReturns400() {
    given().when().get("fulfillment/product/0").then().statusCode(400);
  }

  @Test
  @TestTransaction
  public void testAssign_productNotFoundReturns404() {
    String body = "{\"productId\":999999,\"storeId\":1,\"warehouseBusinessUnitCode\":\"MWH.001\"}";

    given().contentType(ContentType.JSON).body(body).when().post("fulfillment").then().statusCode(404);
  }

  @Test
  @TestTransaction
  public void testAssign_storeNotFoundReturns404() {
    Long productId = createProduct("FULFILL-REST-TEST-P3");
    String body =
        String.format(
            "{\"productId\":%d,\"storeId\":999999,\"warehouseBusinessUnitCode\":\"MWH.001\"}",
            productId);

    given().contentType(ContentType.JSON).body(body).when().post("fulfillment").then().statusCode(404);
  }

  @Test
  @TestTransaction
  public void testAssign_warehouseNotFoundReturns404() {
    Long productId = createProduct("FULFILL-REST-TEST-P4");
    String body =
        String.format(
            "{\"productId\":%d,\"storeId\":1,\"warehouseBusinessUnitCode\":\"NOT.A.WAREHOUSE\"}",
            productId);

    given().contentType(ContentType.JSON).body(body).when().post("fulfillment").then().statusCode(404);
  }

  @Test
  @TestTransaction
  public void testListByStore_storeNotFoundReturns404() {
    given().when().get("fulfillment/store/999999").then().statusCode(404);
  }

  @Test
  @TestTransaction
  public void testListByProduct_productNotFoundReturns404() {
    given().when().get("fulfillment/product/999999").then().statusCode(404);
  }
}
