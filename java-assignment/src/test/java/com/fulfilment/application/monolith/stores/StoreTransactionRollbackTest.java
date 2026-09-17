package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

/**
 * Tests Task 2's actual central guarantee directly, in its negative direction: if the Store's own
 * transaction rolls back, the legacy system must never be contacted at all — not "contacted and
 * then somehow undone," but genuinely never called. {@link StoreLegacySyncListenerTest} already
 * covers the listener's own delegation and failure-handling logic in isolation; this test proves
 * the actual CDI {@code TransactionPhase.AFTER_SUCCESS} wiring behaves correctly end to end,
 * against a real transaction that genuinely rolls back.
 */
@QuarkusTest
public class StoreTransactionRollbackTest {

  @Inject RollbackTestHelper rollbackTestHelper;

  @InjectMock LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Test
  public void testRolledBackTransactionProducesNoLegacySync() {
    var store = new Store("ROLLBACK-TEST-STORE");

    // the Store is persisted and the event is fired inside this call, but it then deliberately
    // throws, forcing the whole transaction to roll back
    assertThrows(
        RuntimeException.class, () -> rollbackTestHelper.persistFireThenForceRollback(store));

    // TransactionPhase.AFTER_SUCCESS must not fire for a transaction that never actually
    // committed — the legacy gateway must have received zero calls
    verifyNoInteractions(legacyStoreManagerGateway);
  }
}
