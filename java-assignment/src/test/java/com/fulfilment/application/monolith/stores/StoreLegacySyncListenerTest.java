package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Focused unit test for the Task 2 fix: the listener correctly delegates each event to the
 * matching legacy-gateway call. The "only fires after commit" guarantee itself comes from CDI's
 * {@code TransactionPhase.AFTER_SUCCESS} mechanism (framework behavior, not something to
 * reimplement in a test) — {@link StoreResourceTest} exercises that mechanism for real, end to
 * end, via actual HTTP requests against a real transaction.
 */
@ExtendWith(MockitoExtension.class)
public class StoreLegacySyncListenerTest {

  @Mock LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Test
  public void testOnStoreCreated_delegatesToCreateOnLegacySystem() {
    var listener = new StoreLegacySyncListener();
    listener.legacyStoreManagerGateway = legacyStoreManagerGateway;

    var store = new Store("TONSTAD");
    listener.onStoreCreated(new StoreCreatedEvent(store));

    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(store);
  }

  @Test
  public void testOnStoreUpdated_delegatesToUpdateOnLegacySystem() {
    var listener = new StoreLegacySyncListener();
    listener.legacyStoreManagerGateway = legacyStoreManagerGateway;

    var store = new Store("KALLAX");
    listener.onStoreUpdated(new StoreUpdatedEvent(store));

    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(store);
  }

  @Test
  public void testOnStoreCreated_gatewayFailureDoesNotPropagate() {
    var listener = new StoreLegacySyncListener();
    listener.legacyStoreManagerGateway = legacyStoreManagerGateway;

    var store = new Store("FAILING-SYNC");
    doThrow(new RuntimeException("legacy system unreachable"))
        .when(legacyStoreManagerGateway)
        .createStoreOnLegacySystem(store);

    // by this point the Store's own DB transaction has already committed successfully — a
    // failure here must be absorbed (logged), not thrown, since there's nothing left to roll
    // back and an uncaught exception from an AFTER_SUCCESS observer has no well-defined recovery
    assertDoesNotThrow(() -> listener.onStoreCreated(new StoreCreatedEvent(store)));
  }

  @Test
  public void testOnStoreUpdated_gatewayFailureDoesNotPropagate() {
    var listener = new StoreLegacySyncListener();
    listener.legacyStoreManagerGateway = legacyStoreManagerGateway;

    var store = new Store("FAILING-SYNC-UPDATE");
    doThrow(new RuntimeException("legacy system unreachable"))
        .when(legacyStoreManagerGateway)
        .updateStoreOnLegacySystem(store);

    assertDoesNotThrow(() -> listener.onStoreUpdated(new StoreUpdatedEvent(store)));
  }
}
