package xyz.riocode.brewery.beer.order.service.services.listeners;

import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import xyz.riocode.brewery.beer.order.service.config.JmsConfig;
import xyz.riocode.brewery.beer.order.service.services.BeerOrderManager;
import xyz.riocode.brewery.common.events.ValidateBeerOrderResultEvent;

@RequiredArgsConstructor
@Component
public class ValidationResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.VALIDATE_BEER_ORDER_RES_QUEUE)
    public void listen(ValidateBeerOrderResultEvent event) {
        beerOrderManager.processValidationResult(event.getOrderId(), event.getIsValid());
    }

}
