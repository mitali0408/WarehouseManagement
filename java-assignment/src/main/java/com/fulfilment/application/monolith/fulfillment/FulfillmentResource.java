package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.StoreRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Path("fulfillment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FulfillmentResource {

  @Inject AssignWarehouseToProductInStoreUseCase assignUseCase;
  @Inject FulfillmentAssignmentRepository repository;
  @Inject ProductRepository productRepository;
  @Inject StoreRepository storeRepository;

  @Context UriInfo uriInfo;

  /** Request payload for assigning a Warehouse as a fulfillment unit of a Product in a Store. */
  public record AssignmentRequest(Long productId, Long storeId, String warehouseBusinessUnitCode) {}

  @POST
  @Transactional
  public Response assign(AssignmentRequest request) {
    if (request == null
            || request.productId() == null
            || request.storeId() == null
            || request.warehouseBusinessUnitCode() == null
            || request.warehouseBusinessUnitCode().isBlank()) {
      throw new WebApplicationException(
              "productId, storeId and warehouseBusinessUnitCode are all required.", 400);
    }
    if (request.productId() <= 0 || request.storeId() <= 0) {
      throw new WebApplicationException("productId and storeId must be positive.", 400);
    }

    WarehouseProductStoreAssociation created =
            assignUseCase.assign(
                    request.productId(), request.storeId(), request.warehouseBusinessUnitCode());

    URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.id)).build();
    return Response.created(location).entity(created).build();
  }

  @GET
  @Path("/{id}")
  public WarehouseProductStoreAssociation getById(@PathParam("id") Long id) {
    if (id == null || id <= 0) {
      throw new WebApplicationException("id must be a positive number.", 400);
    }
    WarehouseProductStoreAssociation association = repository.findById(id);
    if (association == null) {
      throw new WebApplicationException("Fulfillment assignment " + id + " does not exist.", 404);
    }
    return association;
  }

  @DELETE
  @Path("/{id}")
  @Transactional
  public Response delete(@PathParam("id") Long id) {
    if (id == null || id <= 0) {
      throw new WebApplicationException("id must be a positive number.", 400);
    }
    WarehouseProductStoreAssociation association = repository.findById(id);
    if (association == null) {
      throw new WebApplicationException("Fulfillment assignment " + id + " does not exist.", 404);
    }
    repository.delete(association);
    return Response.status(204).build();
  }

  @GET
  @Path("/store/{storeId}")
  public List<WarehouseProductStoreAssociation> listByStore(@PathParam("storeId") Long storeId) {
    if (storeId == null || storeId <= 0) {
      throw new WebApplicationException("storeId must be a positive number.", 400);
    }
    if (storeRepository.findById(storeId) == null) {
      throw new WebApplicationException("Store " + storeId + " does not exist.", 404);
    }
    return repository.list("storeId", storeId);
  }

  @GET
  @Path("/product/{productId}")
  public List<WarehouseProductStoreAssociation> listByProduct(
          @PathParam("productId") Long productId) {
    if (productId == null || productId <= 0) {
      throw new WebApplicationException("productId must be a positive number.", 400);
    }
    if (productRepository.findById(productId) == null) {
      throw new WebApplicationException("Product " + productId + " does not exist.", 404);
    }
    return repository.list("productId", productId);
  }
}