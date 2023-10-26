package xyz.riocode.brewery.common.events;

import lombok.Builder;
import lombok.Data;
import xyz.riocode.brewery.common.model.BeerOrderDto;

@Data
@Builder
public class ValidateBeerOrderEvent {

    private BeerOrderDto beerOrderDto;
}
