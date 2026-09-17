package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Propagates Store changes to the legacy system strictly after the owning database transaction
 * has committed successfully.
 *
 * <p>{@code TransactionPhase.AFTER_SUCCESS} is a CDI guarantee: these observer methods only run
 * once the surrounding {@code @Transactional} transaction has committed. If that transaction
 * rolls back for any reason, these observers never fire at all — so the legacy system can never
 * receive data for a Store that didn't actually get persisted.
 *
 * <p>The reverse case — the DB commit succeeds but the legacy call itself then fails — is handled
 * explicitly here, at the listener boundary, rather than left to whatever the gateway
 * implementation happens to do internally. By this point the database transaction has already
 * committed and cannot be rolled back just because a downstream sync failed, so there is nothing
 * to "undo" — the only correct response is to surface the failure loudly (structured error log,
 * so it can be alerted on) rather than let it disappear silently or crash the request that
 * triggered it. A production system handling this for real would likely follow up with a
 * retry queue or transactional outbox so a failed sync gets retried later instead of only logged
 * once; that's a deliberately separate concern from the timing guarantee this class exists for.
 */
@ApplicationScoped
public class StoreLegacySyncListener {

  private static final Logger LOGGER = Logger.getLogger(StoreLegacySyncListener.class);

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreCreatedEvent event) {
    try {
      legacyStoreManagerGateway.createStoreOnLegacySystem(event.store());
    } catch (RuntimeException e) {
      LOGGER.errorf(
          e,
          "Legacy sync failed for created Store id=%s name=%s — the Store itself was committed"
              + " successfully, but the legacy system may now be out of sync and needs manual"
              + " reconciliation or a retry.",
          event.store().id,
          event.store().name);
    }
  }

  public void onStoreUpdated(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreUpdatedEvent event) {
    try {
      legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store());
    } catch (RuntimeException e) {
      LOGGER.errorf(
          e,
          "Legacy sync failed for updated Store id=%s name=%s — the Store itself was committed"
              + " successfully, but the legacy system may now be out of sync and needs manual"
              + " reconciliation or a retry.",
          event.store().id,
          event.store().name);
    }
  }
}
