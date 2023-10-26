package xyz.riocode.brewery.beer.order.service.services.beer;

import xyz.riocode.brewery.beer.order.service.services.beer.model.BeerDto;

import java.util.Optional;
import java.util.UUID;

public interface BeerService {

    Optional<BeerDto> getById(UUID beerId);
    Optional<BeerDto> getByUpc(String upc);
}
