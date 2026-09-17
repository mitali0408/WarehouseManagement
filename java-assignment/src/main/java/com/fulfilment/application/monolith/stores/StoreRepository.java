package com.fulfilment.application.monolith.stores;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Repository-style access to Store, alongside the existing active-record usage in StoreResource.
 * Exists specifically so callers that need Store lookups injected as a mockable dependency (e.g.
 * AssignWarehouseToProductInStoreUseCase) aren't forced into a static Panache call, which only
 * works inside a running Quarkus context and can't be reliably unit-tested outside one.
 */
@ApplicationScoped
public class StoreRepository implements PanacheRepository<Store> {}