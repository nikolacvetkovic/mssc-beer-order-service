package xyz.riocode.brewery.beer.order.service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrder;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderEvent;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderStatus;
import xyz.riocode.brewery.beer.order.service.repositories.BeerOrderRepository;

@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {

    private final BeerOrderRepository beerOrderRepository;
    private final StateMachineFactory<BeerOrderStatus, BeerOrderEvent> stateMachineFactory;

    @Override
    public BeerOrder newOrder(BeerOrder beerOrder) {
        beerOrder.setOrderStatus(BeerOrderStatus.NEW);
        BeerOrder savedBeerOrder =  beerOrderRepository.save(beerOrder);
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEvent.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEvent event) {
        StateMachine<BeerOrderStatus, BeerOrderEvent> sm = stateMachineFactory.getStateMachine(beerOrder.getId());
        sm.stopReactively().block();
        sm.getStateMachineAccessor().doWithAllRegions(accessor -> {
            accessor.resetStateMachineReactively(
                    new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null))
                    .block();
        });
        sm.startReactively().block();

        Message<BeerOrderEvent> msg = MessageBuilder.withPayload(event).build();
        sm.sendEvent(Mono.just(msg)).subscribe();
    }
}
