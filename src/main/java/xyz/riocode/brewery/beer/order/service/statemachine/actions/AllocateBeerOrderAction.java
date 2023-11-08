package xyz.riocode.brewery.beer.order.service.statemachine.actions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;
import xyz.riocode.brewery.beer.order.service.config.JmsConfig;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrder;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderEvent;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderStatus;
import xyz.riocode.brewery.beer.order.service.repositories.BeerOrderRepository;
import xyz.riocode.brewery.beer.order.service.services.BeerOrderManagerImpl;
import xyz.riocode.brewery.beer.order.service.web.mappers.BeerOrderMapper;
import xyz.riocode.brewery.common.events.AllocateBeerOrderEvent;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class AllocateBeerOrderAction implements Action<BeerOrderStatus, BeerOrderEvent> {

    private final BeerOrderRepository beerOrderRepository;
    private final JmsTemplate jmsTemplate;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatus, BeerOrderEvent> stateContext) {
        String beerOrderId = (String) stateContext.getMessageHeader(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER_PROPERTY);
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(UUID.fromString(beerOrderId));
        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_BEER_ORDER_REQ_QUEUE, AllocateBeerOrderEvent.builder()
                    .beerOrderDto(beerOrderMapper.beerOrderToDto(beerOrder))
                    .build());
        }, () -> log.error("Order not found. Order id: " + beerOrderId));
    }
}
