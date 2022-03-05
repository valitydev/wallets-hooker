package dev.vality.wallets.hooker.utils;

import dev.vality.fistful.webhooker.*;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventTypeUtilsTest {

    @Test
    public void convertEventTypes() {
        EventFilter eventFilter = new EventFilter();
        LinkedHashSet<EventType> types = new LinkedHashSet<>();
        DestinationEventType value = new DestinationEventType();
        value.setCreated(new DestinationCreated());
        types.add(EventType.destination(value));
        eventFilter.setTypes(types);
        WebhookParams event = new WebhookParams();
        event.setEventFilter(eventFilter);
        Set<dev.vality.wallets.hooker.domain.enums.EventType> eventTypes = EventTypeUtils.convertEventTypes(event);

        assertEquals(1, eventTypes.size());
        assertTrue(eventTypes.contains(dev.vality.wallets.hooker.domain.enums.EventType.DESTINATION_CREATED));
    }
}