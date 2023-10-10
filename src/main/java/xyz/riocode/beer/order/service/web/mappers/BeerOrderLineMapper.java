package xyz.riocode.beer.order.service.web.mappers;

import org.mapstruct.DecoratedWith;
import xyz.riocode.beer.order.service.domain.BeerOrderLine;
import xyz.riocode.beer.order.service.web.model.BeerOrderLineDto;
import org.mapstruct.Mapper;

@Mapper(uses = {DateMapper.class})
@DecoratedWith(BeerOrderLineMapperDecorator.class)
public interface BeerOrderLineMapper {
    BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line);

    BeerOrderLine dtoToBeerOrderLine(BeerOrderLineDto dto);
}
