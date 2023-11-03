package xyz.riocode.brewery.beer.order.service.statemachine.interceptors;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrder;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderEvent;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderStatus;
import xyz.riocode.brewery.beer.order.service.repositories.BeerOrderRepository;
import xyz.riocode.brewery.beer.order.service.services.BeerOrderManagerImpl;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class BeerOrderStateChangeInterceptor extends StateMachineInterceptorAdapter<BeerOrderStatus, BeerOrderEvent> {

    private final BeerOrderRepository beerOrderRepository;

    @Transactional
    @Override
    public void preStateChange(State<BeerOrderStatus, BeerOrderEvent> state,
                               Message<BeerOrderEvent> message, Transition<BeerOrderStatus, BeerOrderEvent> transition,
                               StateMachine<BeerOrderStatus, BeerOrderEvent> stateMachine,
                               StateMachine<BeerOrderStatus, BeerOrderEvent> rootStateMachine) {
        Optional.ofNullable(message)
            .flatMap(msg -> Optional.ofNullable((String)msg.getHeaders().get(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER_PROPERTY)))
                            .ifPresent(id -> {
                                BeerOrder beerOrder = beerOrderRepository.findById(UUID.fromString(id))
                                        .orElseThrow(RuntimeException::new);
                                beerOrder.setOrderStatus(state.getId());
                                beerOrderRepository.saveAndFlush(beerOrder);
                            });
    }
}
