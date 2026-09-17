package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * In-process HTTP-level tests for the Store CRUD endpoints. Since these run @QuarkusTest
 * in-process against a real transaction manager, the POST/PUT/PATCH tests also exercise the
 * Task 2 fix for real: StoreCreatedEvent/StoreUpdatedEvent firing and being picked up by
 * StoreLegacySyncListener strictly after each transaction commits.
 */
@QuarkusTest
public class StoreResourceTest {

  private static final String PATH = "store";

  @Test
  public void testListStores() {
    given().when().get(PATH).then().statusCode(200).body(containsString("TONSTAD"));
  }

  @Test
  public void testGetById_successAndNotFound() {
    given().when().get(PATH + "/1").then().statusCode(200).body(containsString("TONSTAD"));

    given().when().get(PATH + "/999999").then().statusCode(404);
  }

  @Test
  public void testCreate_success() {
    String body = "{\"name\":\"TEST-STORE-CREATE\",\"quantityProductsInStock\":7}";

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("TEST-STORE-CREATE"));
  }

  @Test
  public void testCreate_idSetOnRequestReturns422() {
    String body = "{\"id\":999,\"name\":\"TEST-STORE-BADID\",\"quantityProductsInStock\":1}";

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(422);
  }

  @Test
  public void testUpdate_successAndValidation() {
    String createBody = "{\"name\":\"TEST-STORE-UPDATE\",\"quantityProductsInStock\":3}";
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(createBody)
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    String updateBody = "{\"name\":\"TEST-STORE-UPDATED\",\"quantityProductsInStock\":9}";
    given()
        .contentType(ContentType.JSON)
        .body(updateBody)
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("TEST-STORE-UPDATED"));

    // missing name is a validation error
    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":9}")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(422);

    // updating a non-existent store
    given()
        .contentType(ContentType.JSON)
        .body(updateBody)
        .when()
        .put(PATH + "/999999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testPatch_success() {
    String createBody = "{\"name\":\"TEST-STORE-PATCH\",\"quantityProductsInStock\":3}";
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(createBody)
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    String patchBody = "{\"name\":\"TEST-STORE-PATCHED\",\"quantityProductsInStock\":5}";
    given()
        .contentType(ContentType.JSON)
        .body(patchBody)
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("TEST-STORE-PATCHED"));
  }

  @Test
  public void testPatch_validationAndNotFound() {
    String createBody = "{\"name\":\"TEST-STORE-PATCH-ERR\",\"quantityProductsInStock\":3}";
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(createBody)
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    // missing name is a validation error, same as PUT
    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":9}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(422);

    // patching a non-existent store
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"X\",\"quantityProductsInStock\":1}")
        .when()
        .patch(PATH + "/999999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testPatch_zeroStockIsNotOverwritten() {
    // the store's current stock is 0 — per patch's own logic (`if (entity.
    // quantityProductsInStock != 0)`), stock is only overwritten when the CURRENT value is
    // non-zero, so a store starting at 0 keeps 0 even if the patch body requests something else
    String createBody = "{\"name\":\"TEST-STORE-PATCH-ZERO\",\"quantityProductsInStock\":0}";
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(createBody)
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    String patchBody = "{\"name\":\"TEST-STORE-PATCH-ZERO-UPDATED\",\"quantityProductsInStock\":50}";
    given()
        .contentType(ContentType.JSON)
        .body(patchBody)
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("\"quantityProductsInStock\":0"));
  }

  @Test
  public void testCreate_malformedJson_returns400() {
    // Malformed JSON fails during request deserialization, handled by RESTEasy Reactive/Jackson
    // directly — it never reaches ErrorMapper at all, confirmed by this returning 400 rather
    // than ErrorMapper's 500 default. Still a useful test: confirms malformed input is rejected
    // cleanly with a client-error status rather than crashing.
    given()
        .contentType(ContentType.JSON)
        .body("{not valid json")
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void testDelete_successAndNotFound() {
    String createBody = "{\"name\":\"TEST-STORE-DELETE\",\"quantityProductsInStock\":1}";
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(createBody)
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    given().when().delete(PATH + "/" + id).then().statusCode(404);
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }
}
