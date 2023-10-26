package xyz.riocode.brewery.beer.order.service.web.mappers;

import org.mapstruct.DecoratedWith;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderLine;
import xyz.riocode.brewery.common.model.BeerOrderLineDto;
import org.mapstruct.Mapper;

@Mapper(uses = {DateMapper.class})
@DecoratedWith(BeerOrderLineMapperDecorator.class)
public interface BeerOrderLineMapper {
    BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line);

    BeerOrderLine dtoToBeerOrderLine(BeerOrderLineDto dto);
}
