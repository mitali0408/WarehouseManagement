package com.fulfilment.application.monolith.stores;

/** Fired after a Store creation transaction has been requested, for post-commit handling. */
public record StoreCreatedEvent(Store store) {}
