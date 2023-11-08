package xyz.riocode.brewery.beer.order.service.services.testcomponents;

import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import xyz.riocode.brewery.beer.order.service.config.JmsConfig;
import xyz.riocode.brewery.common.events.ValidateBeerOrderEvent;
import xyz.riocode.brewery.common.events.ValidateBeerOrderResultEvent;


@RequiredArgsConstructor
@Component
public class ValidationResultListenerStub {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_BEER_ORDER_REQ_QUEUE)
    public void listen(ValidateBeerOrderEvent event) throws InterruptedException {
        Thread.sleep(500);
        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_BEER_ORDER_RES_QUEUE, ValidateBeerOrderResultEvent.builder()
                .orderId(event.getBeerOrderDto().getId())
                .isValid(true)
                .build());
    }

}
