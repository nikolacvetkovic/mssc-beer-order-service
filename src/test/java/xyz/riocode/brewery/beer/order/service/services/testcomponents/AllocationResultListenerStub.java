package xyz.riocode.brewery.beer.order.service.services.testcomponents;

import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import xyz.riocode.brewery.beer.order.service.config.JmsConfig;
import xyz.riocode.brewery.common.events.AllocateBeerOrderEvent;
import xyz.riocode.brewery.common.events.AllocateBeerOrderResultEvent;
import xyz.riocode.brewery.common.model.BeerOrderDto;

@RequiredArgsConstructor
@Component
public class AllocationResultListenerStub {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_BEER_ORDER_REQ_QUEUE)
    public void listen(AllocateBeerOrderEvent event) {
        event.getBeerOrderDto().getBeerOrderLines().forEach(beerOrderLineDto -> {
            beerOrderLineDto.setAllocatedQuantity(beerOrderLineDto.getOrderQuantity());
        });
        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_BEER_ORDER_RES_QUEUE, AllocateBeerOrderResultEvent.builder()
                .beerOrderDto(event.getBeerOrderDto())
                .allocationError(false)
                .inventoryPending(false)
                .build());
    }
}
