package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Test-only helper that persists a Store, fires the same event StoreResource fires, and then
 * deliberately throws — forcing the transaction to roll back after both of those have happened.
 * This lets {@link StoreTransactionRollbackTest} construct the exact scenario Task 2's guarantee
 * is supposed to protect against and verify it directly, rather than only testing the timing
 * indirectly through a normal successful request.
 */
@ApplicationScoped
public class RollbackTestHelper {

  @Inject Event<StoreCreatedEvent> storeCreatedEvent;

  @Transactional
  public void persistFireThenForceRollback(Store store) {
    store.persist();
    storeCreatedEvent.fire(new StoreCreatedEvent(store));
    throw new RuntimeException("Deliberate failure to force a transaction rollback, for testing.");
  }
}
