package xyz.riocode.brewery.beer.order.service.statemachine.actions;

import lombok.RequiredArgsConstructor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;
import xyz.riocode.brewery.beer.order.service.config.JmsConfig;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderEvent;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderStatus;
import xyz.riocode.brewery.beer.order.service.services.BeerOrderManagerImpl;
import xyz.riocode.brewery.common.events.BeerOrderAllocationFailedEvent;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class BeerOrderAllocationFailedAction implements Action<BeerOrderStatus, BeerOrderEvent> {

    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatus, BeerOrderEvent> stateContext) {
        String beerOrderId = (String) stateContext.getMessageHeader(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER_PROPERTY);
        jmsTemplate.convertAndSend(JmsConfig.BEER_ORDER_ALLOCATION_FAILED_QUEUE, BeerOrderAllocationFailedEvent.builder()
                .orderId(UUID.fromString(beerOrderId))
                .build());
    }
}
