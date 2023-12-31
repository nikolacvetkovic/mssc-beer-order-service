package xyz.riocode.brewery.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;
import xyz.riocode.brewery.beer.order.service.config.JmsConfig;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrder;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderLine;
import xyz.riocode.brewery.beer.order.service.domain.BeerOrderStatus;
import xyz.riocode.brewery.beer.order.service.domain.Customer;
import xyz.riocode.brewery.beer.order.service.repositories.BeerOrderRepository;
import xyz.riocode.brewery.beer.order.service.repositories.CustomerRepository;
import xyz.riocode.brewery.beer.order.service.services.beer.BeerServiceImpl;
import xyz.riocode.brewery.beer.order.service.services.beer.model.BeerDto;
import xyz.riocode.brewery.common.events.BeerOrderAllocationFailedEvent;
import xyz.riocode.brewery.common.events.DeallocateBeerOrderEvent;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class BeerOrderManagerImplIT {

    @Autowired
    BeerOrderManager beerOrderManager;
    @Autowired
    BeerOrderRepository beerOrderRepository;
    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JmsTemplate jmsTemplate;
    @Autowired
    WireMockServer wireMockServer;
    Customer testCustomer;
    UUID beerId = UUID.randomUUID();

    @TestConfiguration
    static class wireMockConfig {
        @Bean
        public WireMockServer wireMockServer() {
            WireMockServer server = new WireMockServer(8083);
            server.start();
            return server;
        }
    }
    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .customerName("Test Customer")
                .build();
        customerRepository.save(testCustomer);
    }

    @Test
    void testNewToAllocated() throws JsonProcessingException, InterruptedException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH + "12345").willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder savedBeerOrder = beerOrderManager.newOrder(createBeerOrder());

        await().until(getBeerOrderStatus(savedBeerOrder.getId()), equalTo(BeerOrderStatus.ALLOCATED));

//        await().until(() -> {
//                    Optional<BeerOrder> foundOrderOptional = beerOrderRepository.findById(savedBeerOrder.getId());
//                    return foundOrderOptional.filter(beerOrder -> beerOrder.getOrderStatus() == BeerOrderStatus.ALLOCATED).isPresent();
//                });
//        await().untilAsserted(() -> {
//            Optional<BeerOrder> foundOrderOptional = beerOrderRepository.findById(beerId);
//            foundOrderOptional.ifPresent(foundOrder -> {
//                BeerOrderLine beerOrderLine = foundOrder.getBeerOrderLines().iterator().next();
//                assertEquals(beerOrderLine.getOrderQuantity(), beerOrderLine.getQuantityAllocated());
//            });
//        });

        // duplicated
        BeerOrder allocatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
        assertEquals(BeerOrderStatus.ALLOCATED, allocatedBeerOrder.getOrderStatus());
    }

    @Test
    void testNewToPickedUp() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH + "12345").willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder savedBeerOrder = beerOrderManager.newOrder(createBeerOrder());

        await().until(getBeerOrderStatus(savedBeerOrder.getId()), equalTo(BeerOrderStatus.ALLOCATED));

        beerOrderManager.beerOrderPickedUp(savedBeerOrder.getId());

        await().until(getBeerOrderStatus(savedBeerOrder.getId()), equalTo(BeerOrderStatus.PICKED_UP));

        // duplicated
        BeerOrder pickedUpBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
        assertEquals(BeerOrderStatus.PICKED_UP, pickedUpBeerOrder.getOrderStatus());
    }

    @Test
    void testNewToValidationException() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH + "12345").willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("fail");

        BeerOrder savedBeerOrder = beerOrderManager.newOrder(beerOrder);

        await().until(getBeerOrderStatus(savedBeerOrder.getId()), equalTo(BeerOrderStatus.VALIDATION_EXCEPTION));
    }

    @Test
    void testNewToTestAllocationFailed() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH + "12345").willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("fail-allocation");

        BeerOrder savedBeerOrder = beerOrderManager.newOrder(beerOrder);

        await().until(getBeerOrderStatus(savedBeerOrder.getId()), equalTo(BeerOrderStatus.ALLOCATION_EXCEPTION));

        BeerOrderAllocationFailedEvent event = (BeerOrderAllocationFailedEvent) jmsTemplate.receiveAndConvert(JmsConfig.BEER_ORDER_ALLOCATION_FAILED_QUEUE);
        assertNotNull(event);
        assertThat(event.getOrderId()).isEqualTo(savedBeerOrder.getId());
    }

    @Test
    void testNewToPartial() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH + "12345").willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("partial-allocation");

        BeerOrder savedBeerOrder = beerOrderManager.newOrder(beerOrder);

        await().until(getBeerOrderStatus(savedBeerOrder.getId()), equalTo(BeerOrderStatus.PENDING_INVENTORY));
    }

    @Test
    void testValidationPendingToCanceled() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH + "12345").willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("dont-validate");

        BeerOrder savedBeerOrder = beerOrderManager.newOrder(beerOrder);

        await().until(getBeerOrderStatus(savedBeerOrder.getId()), equalTo(BeerOrderStatus.VALIDATION_PENDING));

        beerOrderManager.cancelOrder(savedBeerOrder.getId());

        await().until(getBeerOrderStatus(savedBeerOrder.getId()), equalTo(BeerOrderStatus.CANCELED));
    }

    @Test
    void testAllocationPendingToCanceled() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH + "12345").willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("dont-allocate");

        BeerOrder savedBeerOrder = beerOrderManager.newOrder(beerOrder);

        await().until(getBeerOrderStatus(savedBeerOrder.getId()), equalTo(BeerOrderStatus.ALLOCATION_PENDING));

        beerOrderManager.cancelOrder(savedBeerOrder.getId());

        await().until(getBeerOrderStatus(savedBeerOrder.getId()), equalTo(BeerOrderStatus.CANCELED));
    }

    @Test
    void testAllocatedToCanceled() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH + "12345").willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder savedBeerOrder = beerOrderManager.newOrder(createBeerOrder());

        await().until(getBeerOrderStatus(savedBeerOrder.getId()), equalTo(BeerOrderStatus.ALLOCATED));

        beerOrderManager.cancelOrder(savedBeerOrder.getId());

        await().until(getBeerOrderStatus(savedBeerOrder.getId()), equalTo(BeerOrderStatus.CANCELED));

        DeallocateBeerOrderEvent event = (DeallocateBeerOrderEvent) jmsTemplate.receiveAndConvert(JmsConfig.DEALLOCATE_BEER_ORDER_REQ_QUEUE);
        assertNotNull(event);
        assertThat(event.getBeerOrderDto().getId()).isEqualTo(savedBeerOrder.getId());
    }

    BeerOrder createBeerOrder() {
        BeerOrder beerOrder = BeerOrder.builder()
                .customer(testCustomer)
                .build();
        Set<BeerOrderLine> orderLines = new HashSet<>();
        orderLines.add(BeerOrderLine.builder()
                .beerId(beerId)
                .upc("12345")
                .orderQuantity(1)
                .beerOrder(beerOrder)
                .build());
        beerOrder.setBeerOrderLines(orderLines);
        return beerOrder;
    }

    private Callable<BeerOrderStatus> getBeerOrderStatus(UUID beerOrderId) {
        return () -> {
            Optional<BeerOrder> foundOrderOptional = beerOrderRepository.findById(beerOrderId);
            return foundOrderOptional.map(BeerOrder::getOrderStatus).orElse(null);
        };
    }
}
