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
import xyz.riocode.brewery.beer.order.service.statemachine.interceptors.BeerOrderStateChangeInterceptor;
import xyz.riocode.brewery.common.model.BeerOrderDto;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String BEER_ORDER_ID_HEADER_PROPERTY = "beer_order_id";

    private final BeerOrderRepository beerOrderRepository;
    private final StateMachineFactory<BeerOrderStatus, BeerOrderEvent> stateMachineFactory;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

    @Override
    public BeerOrder newOrder(BeerOrder beerOrder) {
        beerOrder.setOrderStatus(BeerOrderStatus.NEW);
        BeerOrder savedBeerOrder =  beerOrderRepository.save(beerOrder);
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEvent.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Override
    public void processValidationResult(UUID orderId, Boolean isValid) {
        BeerOrder beerOrder = beerOrderRepository.findById(orderId).orElseThrow(RuntimeException::new);
        if (isValid) {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.VALIDATION_PASSED);
        } else {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.VALIDATION_FAILED);
        }
    }

    @Override
    public void processAllocationSuccessful(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.findById(beerOrderDto.getId()).orElseThrow(RuntimeException::new);
        sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_SUCCESS);
        updateAllocatedQuantity(beerOrderDto, beerOrder);
    }

    @Override
    public void processAllocationFailed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.findById(beerOrderDto.getId()).orElseThrow(RuntimeException::new);
        sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_FAILED);
    }

    @Override
    public void processAllocationInventoryPending(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.findById(beerOrderDto.getId()).orElseThrow(RuntimeException::new);
        sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_NO_INVENTORY);
        updateAllocatedQuantity(beerOrderDto, beerOrder);
    }

    private void updateAllocatedQuantity(BeerOrderDto beerOrderDto, BeerOrder beerOrder) {
        BeerOrder retrievedOrder = beerOrderRepository.findById(beerOrderDto.getId()).orElseThrow(RuntimeException::new);
        retrievedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
            beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                if (beerOrderLine.getBeerId().equals(beerOrderLineDto.getBeerId())) {
                    beerOrderLine.setQuantityAllocated(beerOrderLineDto.getAllocatedQuantity());
                }
            });
        });
        beerOrderRepository.saveAndFlush(retrievedOrder);
    }

    private StateMachine<BeerOrderStatus, BeerOrderEvent> build(BeerOrder beerOrder) {
        StateMachine<BeerOrderStatus, BeerOrderEvent> sm = stateMachineFactory.getStateMachine(beerOrder.getId());
        sm.stopReactively().block();
        sm.getStateMachineAccessor().doWithAllRegions(accessor -> {
            accessor.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
            accessor.resetStateMachineReactively(
                            new DefaultStateMachineContext<>(beerOrder.getOrderStatus(),
                                    null,
                                    null,
                                    null))
                    .block();
        });
        sm.startReactively().block();
        return sm;
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEvent event) {
        StateMachine<BeerOrderStatus, BeerOrderEvent> sm = build(beerOrder);
        Message<BeerOrderEvent> msg = MessageBuilder.withPayload(event)
                                                    .setHeader(BEER_ORDER_ID_HEADER_PROPERTY, beerOrder.getId())
                                                    .build();
        sm.sendEvent(Mono.just(msg)).subscribe();
    }
}