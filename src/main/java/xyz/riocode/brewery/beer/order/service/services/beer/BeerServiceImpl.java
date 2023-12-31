package xyz.riocode.brewery.beer.order.service.services.beer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xyz.riocode.brewery.beer.order.service.services.beer.model.BeerDto;

import java.util.Optional;
import java.util.UUID;

@Service
public class BeerServiceImpl implements BeerService {

    public final static String BEER_UPC_PATH = "/api/v1/beer/upc/";
    public final static String BEER_ID_PATH = "/api/v1/beer/";

    @Value("${xyz.brewery.beer-service-host}")
    private String beerServiceHost;

    private final RestTemplate restTemplate;

    public BeerServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Optional<BeerDto> getById(UUID beerId) {
        return Optional.ofNullable(restTemplate.getForObject(beerServiceHost + BEER_ID_PATH + beerId,
                                            BeerDto.class));
    }

    @Override
    public Optional<BeerDto> getByUpc(String upc) {
        return Optional.ofNullable(restTemplate.getForObject(beerServiceHost + BEER_UPC_PATH + upc,
                BeerDto.class));
    }
}
