package dev.vality.wallets.hooker.handler.destination;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.fistful.destination.AccountChange;
import dev.vality.fistful.destination.TimestampedChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.dao.destination.DestinationMessageDaoImpl;
import dev.vality.wallets.hooker.dao.destination.DestinationReferenceDao;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.domain.tables.pojos.DestinationIdentityReference;
import dev.vality.wallets.hooker.domain.tables.pojos.DestinationMessage;
import dev.vality.wallets.hooker.exception.HandleEventException;
import dev.vality.wallets.hooker.handler.destination.generator.DestinationCreatedHookMessageGenerator;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class DestinationAccountChangeHandler implements DestinationEventHandler {

    public static final String EXTERNAL_ID = "externalID";

    private final DestinationReferenceDao destinationReferenceDao;
    private final DestinationMessageDaoImpl destinationMessageDao;
    private final DestinationCreatedHookMessageGenerator destinationCreatedHookMessageGenerator;
    private final WebHookDao webHookDao;
    private final WebHookMessageSenderService webHookMessageSenderService;

    private final ObjectMapper objectMapper;

    @Override
    public boolean accept(TimestampedChange change) {
        return change.getChange().isSetAccount()
                && change.getChange().getAccount().isSetCreated();
    }

    @Override
    public void handle(TimestampedChange change, MachineEvent event) {
        try {
            String destinationId = event.getSourceId();
            AccountChange account = change.getChange().getAccount();
            String identityId = account.getCreated().getIdentity();

            log.info("Start handling DestinationAccountCreatedChange: destinationId={}, identityId={}",
                    destinationId, identityId);

            DestinationMessage destinationMessage = destinationMessageDao.get(destinationId);
            createDestinationReference(event, identityId, getExternalId(destinationMessage));

            webHookDao.getByIdentityAndEventType(identityId, EventType.DESTINATION_CREATED)
                    .stream()
                    .map(webhook -> generateDestinationCreateHookMsg(
                            destinationMessage,
                            webhook,
                            destinationId,
                            event.getEventId(),
                            event.getCreatedAt()))
                    .forEach(webHookMessageSenderService::send);

            log.info("Finish handling DestinationAccountCreatedChange: destinationId={}, identityId={}",
                    destinationId, identityId);

        } catch (IOException e) {
            log.error("Error while handling DestinationAccountCreatedChange: {}", change, e);
            throw new HandleEventException("Error while handling DestinationAccountCreatedChange", e);
        }
    }

    private String getExternalId(DestinationMessage destinationMessage) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(destinationMessage.getMessage());
        JsonNode externalID = jsonNode.get(EXTERNAL_ID);
        return externalID.asText();
    }

    private WebhookMessage generateDestinationCreateHookMsg(
            DestinationMessage destinationMessage,
            WebHookModel webhook,
            String sourceId,
            Long eventId,
            String createdAt) {
        MessageGenParams messageGenParams = MessageGenParams.builder()
                .sourceId(sourceId)
                .eventId(eventId)
                .createdAt(createdAt)
                .build();
        return destinationCreatedHookMessageGenerator.generate(destinationMessage, webhook, messageGenParams);
    }

    private void createDestinationReference(MachineEvent event, String identityId, String externalID) {
        DestinationIdentityReference destinationIdentityReference = new DestinationIdentityReference();
        destinationIdentityReference.setDestinationId(event.getSourceId());
        destinationIdentityReference.setIdentityId(identityId);
        destinationIdentityReference.setEventId(String.valueOf(event.getEventId()));
        destinationIdentityReference.setExternalId(externalID);

        destinationReferenceDao.create(destinationIdentityReference);
    }
}
