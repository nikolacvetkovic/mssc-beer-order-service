package xyz.riocode.brewery.beer.order.service.services;

import xyz.riocode.brewery.beer.order.service.domain.BeerOrder;

import java.util.UUID;

public interface BeerOrderManager {
    BeerOrder newOrder(BeerOrder beerOrder);
    void processValidationResult(UUID orderId, Boolean isValid);
}
