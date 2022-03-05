package dev.vality.wallets.hooker.handler.destination;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.fistful.destination.TimestampedChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.swag.wallets.webhook.events.model.Destination;
import dev.vality.wallets.hooker.dao.destination.DestinationMessageDaoImpl;
import dev.vality.wallets.hooker.converter.DestinationToDestinationMessageConverter;
import dev.vality.wallets.hooker.domain.tables.pojos.DestinationMessage;
import dev.vality.wallets.hooker.exception.HandleEventException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DestinationCreatedHandler implements DestinationEventHandler {

    private final DestinationMessageDaoImpl destinationMessageDao;
    private final DestinationToDestinationMessageConverter destinationToDestinationMessageConverter;
    private final ObjectMapper objectMapper;

    @Override
    public boolean accept(TimestampedChange change) {
        return change.getChange().isSetCreated();
    }

    @Override
    public void handle(TimestampedChange change, MachineEvent event) {
        try {
            String destinationId = event.getSourceId();
            log.info("Start handling DestinationCreatedChange: destinationId={}", destinationId);

            Destination destination = destinationToDestinationMessageConverter.convert(change.getChange().getCreated());
            destination.setId(destinationId);

            DestinationMessage destinationMessage = new DestinationMessage();
            destinationMessage.setDestinationId(destinationId);
            destinationMessage.setMessage(objectMapper.writeValueAsString(destination));

            destinationMessageDao.create(destinationMessage);

            log.info("Finish handling DestinationCreatedChange: destinationId={}", destinationId);
        } catch (JsonProcessingException e) {
            log.error("Error while handling DestinationCreatedChange: {}", change, e);
            throw new HandleEventException("Error while handling DestinationCreatedChange", e);
        }
    }

}
