package xyz.riocode.brewery.beer.order.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
public class JmsConfig {

    public static final String VALIDATE_BEER_ORDER_REQ_QUEUE = "validate-beer-order-request";
    public static final String VALIDATE_BEER_ORDER_RES_QUEUE = "validate-beer-order-response";
    public static final String ALLOCATE_BEER_ORDER_REQ_QUEUE = "allocate-beer-order-request";
    public static final String ALLOCATE_BEER_ORDER_RES_QUEUE = "allocate-beer-order-response";
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setObjectMapper(objectMapper);
        return converter;
    }
}
