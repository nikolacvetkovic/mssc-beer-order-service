package xyz.riocode.beer.order.service.web.mappers;

import org.springframework.beans.factory.annotation.Autowired;
import xyz.riocode.beer.order.service.domain.BeerOrderLine;
import xyz.riocode.beer.order.service.services.beer.BeerService;
import xyz.riocode.beer.order.service.services.beer.model.BeerDto;
import xyz.riocode.beer.order.service.web.model.BeerOrderLineDto;

import java.util.Optional;

public abstract class BeerOrderLineMapperDecorator implements BeerOrderLineMapper{

    @Autowired
    private BeerOrderLineMapper beerOrderLineMapper;
    @Autowired
    private BeerService beerService;

    @Override
    public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line) {
        BeerOrderLineDto beerOrderLineDto = beerOrderLineMapper.beerOrderLineToDto(line);
        Optional<BeerDto> beerDtoOptional = beerService.getByUpc(line.getUpc());

        beerDtoOptional.ifPresent(beerDto -> {
            beerOrderLineDto.setBeerName(beerDto.getBeerName());
            beerOrderLineDto.setBeerStyle(beerDto.getBeerStyle());
            beerOrderLineDto.setBeerId(beerDto.getId());
            beerOrderLineDto.setPrice(beerDto.getPrice());
        });

        return beerOrderLineDto;
    }
}
