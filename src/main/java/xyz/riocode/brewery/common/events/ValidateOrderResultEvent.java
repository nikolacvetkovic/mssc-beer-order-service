package xyz.riocode.brewery.common.events;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ValidateOrderResultEvent {
    private UUID orderId;
    private Boolean isValid;
}
