package xyz.riocode.brewery.beer.order.service.services;

import xyz.riocode.brewery.beer.order.service.domain.BeerOrder;
import xyz.riocode.brewery.common.model.BeerOrderDto;

import java.util.UUID;

public interface BeerOrderManager {
    BeerOrder newOrder(BeerOrder beerOrder);
    void processValidationResult(UUID orderId, Boolean isValid);
    void processAllocationSuccessful(BeerOrderDto beerOrderDto);
    void processAllocationFailed(BeerOrderDto beerOrderDto);
    void processAllocationInventoryPending(BeerOrderDto beerOrderDto);
}
