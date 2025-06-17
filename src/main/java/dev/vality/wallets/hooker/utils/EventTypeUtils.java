package dev.vality.wallets.hooker.utils;

import dev.vality.fistful.webhooker.DestinationEventType;
import dev.vality.fistful.webhooker.WebhookParams;
import dev.vality.fistful.webhooker.WithdrawalEventType;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.exception.UnknownEventTypeException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventTypeUtils {

    public static Set<EventType> convertEventTypes(WebhookParams event) {
        return event.getEventFilter().getTypes().stream()
                .map(EventTypeUtils::resolveEventType)
                .collect(Collectors.toSet());
    }

    private static EventType resolveEventType(dev.vality.fistful.webhooker.EventType type) {
        if (type.isSetWithdrawal()) {
            WithdrawalEventType withdrawal = type.getWithdrawal();
            if (withdrawal.isSetFailed()) {
                return EventType.WITHDRAWAL_FAILED;
            } else if (withdrawal.isSetStarted()) {
                return EventType.WITHDRAWAL_CREATED;
            } else if (withdrawal.isSetSucceeded()) {
                return EventType.WITHDRAWAL_SUCCEEDED;
            }
        } else if (type.isSetDestination()) {
            DestinationEventType destination = type.getDestination();
            if (destination.isSetCreated()) {
                return EventType.DESTINATION_CREATED;
            }
        }
        throw new UnknownEventTypeException(type.toString());
    }

}
