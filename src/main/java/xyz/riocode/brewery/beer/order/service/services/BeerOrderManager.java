package xyz.riocode.brewery.beer.order.service.services;

import xyz.riocode.brewery.beer.order.service.domain.BeerOrder;

public interface BeerOrderManager {
    BeerOrder newOrder(BeerOrder beerOrder);
}
