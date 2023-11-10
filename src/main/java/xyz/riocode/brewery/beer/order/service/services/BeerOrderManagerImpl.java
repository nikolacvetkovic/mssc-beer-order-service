package xyz.riocode.brewery.beer.order.service.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrder;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderEvent;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderStatus;
import xyz.riocode.brewery.beer.order.service.repositories.BeerOrderRepository;
import xyz.riocode.brewery.beer.order.service.statemachine.interceptors.BeerOrderStateChangeInterceptor;
import xyz.riocode.brewery.common.model.BeerOrderDto;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String BEER_ORDER_ID_HEADER_PROPERTY = "beer_order_id";

    private final BeerOrderRepository beerOrderRepository;
    private final StateMachineFactory<BeerOrderStatus, BeerOrderEvent> stateMachineFactory;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;
    private final EntityManager entityManager;

    @Transactional
    @Override
    public BeerOrder newOrder(BeerOrder beerOrder) {
        beerOrder.setOrderStatus(BeerOrderStatus.NEW);
        BeerOrder savedBeerOrder =  beerOrderRepository.saveAndFlush(beerOrder);
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEvent.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Transactional
    @Override
    public void processValidationResult(UUID orderId, Boolean isValid) {
        beerOrderRepository.findById(orderId).ifPresentOrElse(beerOrder -> {
            if (isValid) {
                sendBeerOrderEvent(beerOrder, BeerOrderEvent.VALIDATION_PASSED);
                BeerOrder validatedOrder = beerOrderRepository.findById(orderId).get();
                sendBeerOrderEvent(validatedOrder, BeerOrderEvent.ALLOCATE_ORDER);
            } else {
                sendBeerOrderEvent(beerOrder, BeerOrderEvent.VALIDATION_FAILED);
            }
        }, () -> log.error("processValidationOrder -> Order not found. Order id: " + orderId));

    }

    @Override
    public void processAllocationSuccessful(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_SUCCESS);
            updateAllocatedQuantity(beerOrderDto);
        }, () -> log.error("processAllocationSuccessful -> Order not found. Order id: " + beerOrderDto.getId()));
    }

    @Override
    public void processAllocationFailed(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_FAILED);
        }, () -> log.error("processAllocationFailed -> Order not found. Order id: " + beerOrderDto.getId()));
    }

    @Override
    public void beerOrderPickedUp(UUID beerOrderId) {
        beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.BEER_ORDER_PICKED_UP);
        }, () -> log.error("beerOrderPickedUp -> Order not found. Order id: " + beerOrderId));
    }

    @Override
    public void processAllocationInventoryPending(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.ALLOCATION_NO_INVENTORY);
            updateAllocatedQuantity(beerOrderDto);
        }, () -> log.error("processAllocationInventoryPending -> Order not found. Order id: " + beerOrderDto.getId()));
    }

    @Override
    public void cancelOrder(UUID beerOrderId) {
        beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEvent.CANCEL_ORDER);
        }, () -> log.error("cancelOrder -> Order not found. Order id: " + beerOrderId));
    }

    private void updateAllocatedQuantity(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> allocatedOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        allocatedOrderOptional.ifPresentOrElse(allocatedOrder -> {
            allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
                beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                    if (beerOrderLine.getBeerId().equals(beerOrderLineDto.getBeerId())) {
                        beerOrderLine.setQuantityAllocated(beerOrderLineDto.getAllocatedQuantity());
                    }
                });
            });
            beerOrderRepository.saveAndFlush(allocatedOrder);
        }, () -> log.error("Order not found. Order id: " + beerOrderDto.getId()));
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
                                                    .setHeader(BEER_ORDER_ID_HEADER_PROPERTY, beerOrder.getId().toString())
                                                    .build();
        sm.sendEvent(Mono.just(msg)).subscribe();
    }
}
