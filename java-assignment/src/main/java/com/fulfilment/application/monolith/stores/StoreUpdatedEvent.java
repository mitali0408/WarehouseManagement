package com.fulfilment.application.monolith.stores;

/** Fired after a Store update transaction has been requested, for post-commit handling. */
public record StoreUpdatedEvent(Store store) {}
