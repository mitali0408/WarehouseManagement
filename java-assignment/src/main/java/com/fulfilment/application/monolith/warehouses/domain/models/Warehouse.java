package com.fulfilment.application.monolith.warehouses.domain.models;

import java.time.LocalDateTime;

public class Warehouse {

  // database row identifier — used for direct REST addressing (GET/DELETE by id);
  // distinct from businessUnitCode, which is the durable identity reused across replace history
  public Long id;

  // unique identifier
  public String businessUnitCode;

  public String location;

  public Integer capacity;

  public Integer stock;

  public LocalDateTime createdAt;

  public LocalDateTime archivedAt;
}
