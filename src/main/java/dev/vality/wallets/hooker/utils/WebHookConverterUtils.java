package dev.vality.wallets.hooker.utils;

import dev.vality.fistful.webhooker.*;
import dev.vality.wallets.hooker.exception.UnknownEventTypeException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WebHookConverterUtils {

    public static EventFilter generateEventFilter(Set<dev.vality.wallets.hooker.domain.enums.EventType> eventTypes) {
        EventFilter eventFilter = new EventFilter();
        if (eventTypes != null) {
            eventFilter.setTypes(eventTypes.stream()
                    .map(WebHookConverterUtils::resolveEventType)
                    .collect(Collectors.toSet()));
        } else {
            eventFilter.setTypes(Collections.emptySet());
        }
        return eventFilter;
    }

    private static EventType resolveEventType(dev.vality.wallets.hooker.domain.enums.EventType type) {
        return switch (type) {
            case WITHDRAWAL_CREATED -> EventType.withdrawal(WithdrawalEventType.started(new WithdrawalStarted()));
            case WITHDRAWAL_FAILED -> EventType.withdrawal(WithdrawalEventType.failed(new WithdrawalFailed()));
            case WITHDRAWAL_SUCCEEDED -> EventType.withdrawal(WithdrawalEventType.succeeded(new WithdrawalSucceeded()));
            case DESTINATION_CREATED -> EventType.destination(DestinationEventType.created(new DestinationCreated()));
            default -> throw new UnknownEventTypeException();
        };
    }

}
