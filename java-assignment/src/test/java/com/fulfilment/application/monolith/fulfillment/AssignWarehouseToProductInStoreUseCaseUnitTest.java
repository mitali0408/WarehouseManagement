package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AssignWarehouseToProductInStoreUseCaseUnitTest {

    @Mock FulfillmentAssignmentStore assignmentStore;
    @Mock WarehouseStore warehouseStore;
    @Mock ProductRepository productRepository;
    @Mock StoreRepository storeRepository;

    private AssignWarehouseToProductInStoreUseCase useCase() {
        return new AssignWarehouseToProductInStoreUseCase(
                assignmentStore, warehouseStore, productRepository, storeRepository);
    }

    private static Warehouse activeWarehouse(String code) {
        var warehouse = new Warehouse();
        warehouse.businessUnitCode = code;
        warehouse.location = "ZWOLLE-001";
        warehouse.capacity = 40;
        warehouse.stock = 10;
        return warehouse;
    }

    @Test
    public void testAssign_succeeds() {
        when(productRepository.findById(1L)).thenReturn(new Product("EXISTS"));
        when(storeRepository.findById(1L)).thenReturn(new Store("EXISTS"));
        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(activeWarehouse("MWH.001"));
        when(assignmentStore.existsExact(1L, 1L, "MWH.001")).thenReturn(false);
        when(assignmentStore.countDistinctWarehousesForProductInStore(1L, 1L)).thenReturn(0L);
        when(assignmentStore.warehouseAlreadyServesStore(1L, "MWH.001")).thenReturn(false);
        when(assignmentStore.countDistinctWarehousesForStore(1L)).thenReturn(0L);
        when(assignmentStore.warehouseAlreadyHandlesProduct("MWH.001", 1L)).thenReturn(false);
        when(assignmentStore.countDistinctProductsForWarehouse("MWH.001")).thenReturn(0L);

        WarehouseProductStoreAssociation result = useCase().assign(1L, 1L, "MWH.001");

        assertNotNull(result);
        verify(assignmentStore).save(any());
    }

    @Test
    public void testAssign_throwsWhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(null);

        assertThrows(ProductNotFoundException.class, () -> useCase().assign(999L, 1L, "MWH.001"));
        verify(assignmentStore, never()).save(any());
    }

    @Test
    public void testAssign_throwsWhenStoreNotFound() {
        when(productRepository.findById(1L)).thenReturn(new Product("EXISTS"));
        when(storeRepository.findById(999L)).thenReturn(null);

        assertThrows(StoreNotFoundException.class, () -> useCase().assign(1L, 999L, "MWH.001"));
        verify(assignmentStore, never()).save(any());
    }

    @Test
    public void testAssign_throwsWhenWarehouseNotFound() {
        when(productRepository.findById(1L)).thenReturn(new Product("EXISTS"));
        when(storeRepository.findById(1L)).thenReturn(new Store("EXISTS"));
        when(warehouseStore.findByBusinessUnitCode("NOT.A.WAREHOUSE")).thenReturn(null);

        assertThrows(
                WarehouseNotFoundException.class, () -> useCase().assign(1L, 1L, "NOT.A.WAREHOUSE"));
        verify(assignmentStore, never()).save(any());
    }

    @Test
    public void testAssign_throwsOnExactDuplicate() {
        when(productRepository.findById(1L)).thenReturn(new Product("EXISTS"));
        when(storeRepository.findById(1L)).thenReturn(new Store("EXISTS"));
        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(activeWarehouse("MWH.001"));
        when(assignmentStore.existsExact(1L, 1L, "MWH.001")).thenReturn(true);

        assertThrows(
                DuplicateFulfillmentAssignmentException.class, () -> useCase().assign(1L, 1L, "MWH.001"));
        verify(assignmentStore, never()).save(any());
    }

    @Test
    public void testAssign_throwsWhenProductAlreadyAtMaxWarehousesForStore() {
        when(productRepository.findById(1L)).thenReturn(new Product("EXISTS"));
        when(storeRepository.findById(1L)).thenReturn(new Store("EXISTS"));
        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(activeWarehouse("MWH.001"));
        when(assignmentStore.existsExact(1L, 1L, "MWH.001")).thenReturn(false);
        when(assignmentStore.countDistinctWarehousesForProductInStore(1L, 1L)).thenReturn(2L);

        assertThrows(
                TooManyWarehousesForProductInStoreException.class,
                () -> useCase().assign(1L, 1L, "MWH.001"));
        verify(assignmentStore, never()).save(any());
    }

    @Test
    public void testAssign_throwsWhenStoreAlreadyAtMaxWarehouses() {
        when(productRepository.findById(1L)).thenReturn(new Product("EXISTS"));
        when(storeRepository.findById(1L)).thenReturn(new Store("EXISTS"));
        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(activeWarehouse("MWH.001"));
        when(assignmentStore.existsExact(1L, 1L, "MWH.001")).thenReturn(false);
        when(assignmentStore.countDistinctWarehousesForProductInStore(1L, 1L)).thenReturn(0L);
        when(assignmentStore.warehouseAlreadyServesStore(1L, "MWH.001")).thenReturn(false);
        when(assignmentStore.countDistinctWarehousesForStore(1L)).thenReturn(3L);

        assertThrows(
                TooManyWarehousesForStoreException.class, () -> useCase().assign(1L, 1L, "MWH.001"));
        verify(assignmentStore, never()).save(any());
    }

    @Test
    public void testAssign_throwsWhenWarehouseAlreadyAtMaxProductTypes() {
        when(productRepository.findById(1L)).thenReturn(new Product("EXISTS"));
        when(storeRepository.findById(1L)).thenReturn(new Store("EXISTS"));
        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(activeWarehouse("MWH.001"));
        when(assignmentStore.existsExact(1L, 1L, "MWH.001")).thenReturn(false);
        when(assignmentStore.countDistinctWarehousesForProductInStore(1L, 1L)).thenReturn(0L);
        when(assignmentStore.warehouseAlreadyServesStore(1L, "MWH.001")).thenReturn(false);
        when(assignmentStore.countDistinctWarehousesForStore(1L)).thenReturn(0L);
        when(assignmentStore.warehouseAlreadyHandlesProduct("MWH.001", 1L)).thenReturn(false);
        when(assignmentStore.countDistinctProductsForWarehouse("MWH.001")).thenReturn(5L);

        assertThrows(
                TooManyProductTypesInWarehouseException.class, () -> useCase().assign(1L, 1L, "MWH.001"));
        verify(assignmentStore, never()).save(any());
    }

    @Test
    public void testAssign_succeedsWhenWarehouseAlreadyServesStore_skipsStoreCountCheck() {
        when(productRepository.findById(1L)).thenReturn(new Product("EXISTS"));
        when(storeRepository.findById(1L)).thenReturn(new Store("EXISTS"));
        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(activeWarehouse("MWH.001"));
        when(assignmentStore.existsExact(1L, 1L, "MWH.001")).thenReturn(false);
        when(assignmentStore.countDistinctWarehousesForProductInStore(1L, 1L)).thenReturn(0L);
        when(assignmentStore.warehouseAlreadyServesStore(1L, "MWH.001")).thenReturn(true);
        when(assignmentStore.warehouseAlreadyHandlesProduct("MWH.001", 1L)).thenReturn(false);
        when(assignmentStore.countDistinctProductsForWarehouse("MWH.001")).thenReturn(0L);

        WarehouseProductStoreAssociation result = useCase().assign(1L, 1L, "MWH.001");

        assertNotNull(result);
        verify(assignmentStore, never()).countDistinctWarehousesForStore(any());
    }
}