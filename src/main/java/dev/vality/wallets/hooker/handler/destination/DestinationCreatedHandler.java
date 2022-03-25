package dev.vality.wallets.hooker.handler.destination;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vality.fistful.destination.TimestampedChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.swag.wallets.webhook.events.model.Destination;
import dev.vality.wallets.hooker.converter.DestinationToDestinationMessageConverter;
import dev.vality.wallets.hooker.converter.ResourceToJsonStringDestinationConverter;
import dev.vality.wallets.hooker.dao.destination.DestinationMessageDaoImpl;
import dev.vality.wallets.hooker.domain.tables.pojos.DestinationMessage;
import dev.vality.wallets.hooker.exception.HandleEventException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DestinationCreatedHandler implements DestinationEventHandler {

    public static final String RESOURCE = "resource";
    private final DestinationMessageDaoImpl destinationMessageDao;
    private final DestinationToDestinationMessageConverter destinationToDestinationMessageConverter;
    private final ObjectMapper objectMapper;
    private final ResourceToJsonStringDestinationConverter resourceToJsonStringDestinationConverter;

    @Override
    public boolean accept(TimestampedChange change) {
        return change.getChange().isSetCreated();
    }

    @Override
    public void handle(TimestampedChange change, MachineEvent event) {
        try {
            String destinationId = event.getSourceId();
            log.info("Start handling DestinationCreatedChange: destinationId={}", destinationId);

            dev.vality.fistful.destination.Destination created = change.getChange().getCreated();
            Destination destination = destinationToDestinationMessageConverter.convert(created);
            destination.setId(destinationId);

            DestinationMessage destinationMessage = new DestinationMessage();
            destinationMessage.setDestinationId(destinationId);
            destinationMessage.setMessage(initDestinationMessage(created, destination));

            destinationMessageDao.create(destinationMessage);

            log.info("Finish handling DestinationCreatedChange: destinationId={}", destinationId);
        } catch (JsonProcessingException e) {
            log.error("Error while handling DestinationCreatedChange: {}", change, e);
            throw new HandleEventException("Error while handling DestinationCreatedChange", e);
        }
    }

    private String initDestinationMessage(dev.vality.fistful.destination.Destination created, Destination destination)
            throws JsonProcessingException {
        String message = objectMapper.writeValueAsString(destination);
        JsonNode jsonNodeRoot = objectMapper.readTree(message);
        JsonNode destinationResourceJson = objectMapper.readTree(
                resourceToJsonStringDestinationConverter.convert(created.getResource()));
        JsonNode resultDestinationNode = ((ObjectNode) jsonNodeRoot).set(RESOURCE, destinationResourceJson);
        return objectMapper.writeValueAsString(resultDestinationNode);
    }

}
