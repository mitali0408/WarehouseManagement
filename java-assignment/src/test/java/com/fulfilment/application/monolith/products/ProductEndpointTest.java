package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  @TestTransaction
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Delete the TONSTAD:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, TONSTAD should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  @TestTransaction
  public void testCreateAndGetById() {
    final String path = "product";
    String body = "{\"name\":\"TEST-PRODUCT-CREATE\",\"stock\":12}";

    String id =
        given()
            .contentType("application/json")
            .body(body)
            .when()
            .post(path)
            .then()
            .statusCode(201)
            .body(containsString("TEST-PRODUCT-CREATE"))
            .extract()
            .path("id")
            .toString();

    given()
        .when()
        .get(path + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("TEST-PRODUCT-CREATE"));

    given().when().get(path + "/999999").then().statusCode(404);
  }

  @Test
  @TestTransaction
  public void testCreate_idSetOnRequestReturns422() {
    final String path = "product";
    String body = "{\"id\":999,\"name\":\"TEST-PRODUCT-BADID\",\"stock\":1}";

    given().contentType("application/json").body(body).when().post(path).then().statusCode(422);
  }

  @Test
  @TestTransaction
  public void testUpdate_successAndErrors() {
    final String path = "product";
    String createBody = "{\"name\":\"TEST-PRODUCT-UPDATE\",\"stock\":4}";

    String id =
        given()
            .contentType("application/json")
            .body(createBody)
            .when()
            .post(path)
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    String updateBody = "{\"name\":\"TEST-PRODUCT-UPDATED\",\"stock\":8}";
    given()
        .contentType("application/json")
        .body(updateBody)
        .when()
        .put(path + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("TEST-PRODUCT-UPDATED"));

    // missing name is a validation error
    given()
        .contentType("application/json")
        .body("{\"stock\":8}")
        .when()
        .put(path + "/" + id)
        .then()
        .statusCode(422);

    // updating a non-existent product
    given()
        .contentType("application/json")
        .body(updateBody)
        .when()
        .put(path + "/999999")
        .then()
        .statusCode(404);
  }

  @Test
  @TestTransaction
  public void testDelete_notFoundReturns404() {
    final String path = "product";

    given().when().delete(path + "/999999").then().statusCode(404);
  }

  @Test
  @TestTransaction
  public void testCreate_malformedJson_returns400() {
    // Malformed JSON fails during request deserialization, handled by RESTEasy Reactive/Jackson
    // directly — it never reaches ErrorMapper at all, confirmed by this returning 400 rather
    // than ErrorMapper's 500 default. Still a useful test: confirms malformed input is rejected
    // cleanly with a client-error status rather than crashing.
    final String path = "product";

    given()
        .contentType("application/json")
        .body("{not valid json")
        .when()
        .post(path)
        .then()
        .statusCode(400);
  }
}
