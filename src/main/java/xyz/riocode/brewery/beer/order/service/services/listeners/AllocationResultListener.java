package xyz.riocode.brewery.beer.order.service.services.listeners;

import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import xyz.riocode.brewery.beer.order.service.config.JmsConfig;
import xyz.riocode.brewery.beer.order.service.services.BeerOrderManager;
import xyz.riocode.brewery.common.events.AllocateBeerOrderResultEvent;

@RequiredArgsConstructor
@Component
public class AllocationResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.ALLOCATE_BEER_ORDER_RES_QUEUE)
    public void listen(AllocateBeerOrderResultEvent event) {
        if(!event.getAllocationError() && !event.getInventoryPending()) {
            beerOrderManager.processAllocationSuccessful(event.getBeerOrderDto());
        } else if (!event.getAllocationError() && event.getInventoryPending()) {
            beerOrderManager.processAllocationInventoryPending(event.getBeerOrderDto());
        } else if (event.getAllocationError()) {
            beerOrderManager.processAllocationFailed(event.getBeerOrderDto());
        }
    }
}
