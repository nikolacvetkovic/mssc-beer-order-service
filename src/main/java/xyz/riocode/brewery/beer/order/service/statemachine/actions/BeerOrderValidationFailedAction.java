package xyz.riocode.brewery.beer.order.service.statemachine.actions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderEvent;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderStatus;
import xyz.riocode.brewery.beer.order.service.services.BeerOrderManagerImpl;

@Slf4j
@Component
public class BeerOrderValidationFailedAction implements Action<BeerOrderStatus, BeerOrderEvent> {

    @Override
    public void execute(StateContext<BeerOrderStatus, BeerOrderEvent> stateContext) {
        String beerOrderId = (String) stateContext.getMessageHeader(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER_PROPERTY);
        log.error("Validation failed for order id: " + beerOrderId);
    }
}
