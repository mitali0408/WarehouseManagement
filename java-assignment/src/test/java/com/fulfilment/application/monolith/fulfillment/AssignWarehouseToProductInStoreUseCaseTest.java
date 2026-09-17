package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

/**
 * In-process tests against the seeded/dev-services database, covering the three quantity
 * constraints plus duplicate/not-found handling. Each {@code @Test} method is annotated
 * {@code @TestTransaction}, so its writes roll back automatically at the end — genuine isolation
 * from other tests and from execution order.
 *
 * <p>Products are created fresh per test rather than reusing seeded product ids — seeded rows
 * are shared, mutable state that other test classes (e.g. ProductEndpointTest, which deletes
 * seeded product 1) also touch, and depending on one is fragile even when {@code @TestTransaction}
 * happens to make it safe today. Stores and warehouses are still referenced by their seeded ids,
 * since no test class ever mutates those specific seeded rows.
 */
@QuarkusTest
public class AssignWarehouseToProductInStoreUseCaseTest {

  @Inject AssignWarehouseToProductInStoreUseCase useCase;
  @Inject WarehouseRepository warehouseRepository;
  @Inject ProductRepository productRepository;

  @Transactional
  Long createProduct(String name) {
    var product = new Product(name);
    productRepository.persist(product);
    return product.id;
  }

  @Transactional
  void createWarehouse(String code, String location, int capacity) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = 0;
    warehouse.createdAt = java.time.LocalDateTime.now();
    warehouseRepository.create(warehouse);
  }

  @Test
  @TestTransaction
  public void testAssignSucceeds_thenRejectsExactDuplicate() {
    Long productId = createProduct("FULFILL-TEST-DUP");

    useCase.assign(productId, 1L, "MWH.001");

    assertThrows(
        DuplicateFulfillmentAssignmentException.class,
        () -> useCase.assign(productId, 1L, "MWH.001"));
  }

  @Test
  @TestTransaction
  public void testAssignFails_whenProductDoesNotExist() {
    assertThrows(ProductNotFoundException.class, () -> useCase.assign(999999L, 1L, "MWH.001"));
  }

  @Test
  @TestTransaction
  public void testAssignFails_whenStoreDoesNotExist() {
    Long productId = createProduct("FULFILL-TEST-NOSTORE");

    assertThrows(
        StoreNotFoundException.class, () -> useCase.assign(productId, 999999L, "MWH.001"));
  }

  @Test
  @TestTransaction
  public void testAssignFails_whenWarehouseDoesNotExist() {
    Long productId = createProduct("FULFILL-TEST-NOWH");

    assertThrows(
        WarehouseNotFoundException.class,
        () -> useCase.assign(productId, 1L, "NOT.A.WAREHOUSE"));
  }

  @Test
  @TestTransaction
  public void testAssignFails_exceedsMaxWarehousesPerProductPerStore() {
    Long productId = createProduct("FULFILL-TEST-MAXPERSTORE");

    // product at store 1: 2 distinct warehouses is the max
    useCase.assign(productId, 1L, "MWH.001");
    useCase.assign(productId, 1L, "MWH.012");

    assertThrows(
        TooManyWarehousesForProductInStoreException.class,
        () -> useCase.assign(productId, 1L, "MWH.023"));
  }

  @Test
  @TestTransaction
  public void testAssignSucceedsAtExactlyMaxWarehousesPerProductPerStore() {
    // boundary case: the 2nd distinct warehouse for one product at one store is still within
    // the max of 2, and must succeed (not just "not yet exceed")
    Long productId = createProduct("FULFILL-TEST-MAXPERSTORE-BOUNDARY");

    useCase.assign(productId, 1L, "MWH.001");
    useCase.assign(productId, 1L, "MWH.012");

    // no exception — both assignments succeeded, at exactly the limit
  }

  @Test
  @TestTransaction
  public void testAssignFails_exceedsMaxWarehousesPerStore() {
    Long p1 = createProduct("FULFILL-TEST-STORE3-P1");
    Long p2 = createProduct("FULFILL-TEST-STORE3-P2");
    Long p3 = createProduct("FULFILL-TEST-STORE3-P3");

    // store 3: fill it up to 3 distinct warehouses across 3 different products
    useCase.assign(p1, 3L, "MWH.001");
    useCase.assign(p2, 3L, "MWH.012");
    useCase.assign(p3, 3L, "MWH.023");

    createWarehouse("MWH.777", "AMSTERDAM-002", 20);

    // a 4th distinct warehouse for store 3 (even for a product already served there by another
    // warehouse) must be rejected — the store itself, not just the product, is at its limit
    assertThrows(
        TooManyWarehousesForStoreException.class, () -> useCase.assign(p1, 3L, "MWH.777"));
  }

  @Test
  @TestTransaction
  public void testAssignSucceedsAtExactlyMaxWarehousesPerStore() {
    // boundary case: the 3rd distinct warehouse for a store is still within the max of 3
    Long p1 = createProduct("FULFILL-TEST-STORE3-BOUNDARY-P1");
    Long p2 = createProduct("FULFILL-TEST-STORE3-BOUNDARY-P2");
    Long p3 = createProduct("FULFILL-TEST-STORE3-BOUNDARY-P3");

    useCase.assign(p1, 3L, "MWH.001");
    useCase.assign(p2, 3L, "MWH.012");
    useCase.assign(p3, 3L, "MWH.023");

    // no exception — 3 distinct warehouses at store 3, exactly at the limit
  }

  @Test
  @TestTransaction
  public void testAssignFails_exceedsMaxProductTypesPerWarehouse() {
    createWarehouse("MWH.555", "AMSTERDAM-001", 50);

    Long p4 = createProduct("FULFILL-TEST-P4");
    Long p5 = createProduct("FULFILL-TEST-P5");
    Long p6 = createProduct("FULFILL-TEST-P6");
    Long p7 = createProduct("FULFILL-TEST-P7");
    Long p8 = createProduct("FULFILL-TEST-P8");
    Long p9 = createProduct("FULFILL-TEST-P9");

    // MWH.555 fulfilling 5 distinct products at store 2 is the max
    useCase.assign(p4, 2L, "MWH.555");
    useCase.assign(p5, 2L, "MWH.555");
    useCase.assign(p6, 2L, "MWH.555");
    useCase.assign(p7, 2L, "MWH.555");
    useCase.assign(p8, 2L, "MWH.555");

    // a 6th distinct product for this warehouse must be rejected
    assertThrows(
        TooManyProductTypesInWarehouseException.class, () -> useCase.assign(p9, 2L, "MWH.555"));
  }

  @Test
  @TestTransaction
  public void testAssignSucceedsAtExactlyMaxProductTypesPerWarehouse() {
    // boundary case: the 5th distinct product for a warehouse is still within the max of 5
    createWarehouse("MWH.556", "AMSTERDAM-001", 50);

    Long p1 = createProduct("FULFILL-TEST-MAXPRODUCTS-BOUNDARY-P1");
    Long p2 = createProduct("FULFILL-TEST-MAXPRODUCTS-BOUNDARY-P2");
    Long p3 = createProduct("FULFILL-TEST-MAXPRODUCTS-BOUNDARY-P3");
    Long p4 = createProduct("FULFILL-TEST-MAXPRODUCTS-BOUNDARY-P4");
    Long p5 = createProduct("FULFILL-TEST-MAXPRODUCTS-BOUNDARY-P5");

    useCase.assign(p1, 2L, "MWH.556");
    useCase.assign(p2, 2L, "MWH.556");
    useCase.assign(p3, 2L, "MWH.556");
    useCase.assign(p4, 2L, "MWH.556");
    useCase.assign(p5, 2L, "MWH.556");

    // no exception — 5 distinct products for MWH.556, exactly at the limit
  }
}
