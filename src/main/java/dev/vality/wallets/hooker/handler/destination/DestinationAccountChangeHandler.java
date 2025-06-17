package dev.vality.wallets.hooker.handler.destination;

import dev.vality.fistful.destination.AccountChange;
import dev.vality.fistful.destination.TimestampedChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.dao.destination.DestinationMessageDaoImpl;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.domain.tables.pojos.DestinationMessage;
import dev.vality.wallets.hooker.exception.HandleEventException;
import dev.vality.wallets.hooker.handler.destination.generator.DestinationCreatedHookMessageGenerator;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DestinationAccountChangeHandler implements DestinationEventHandler {

    private final DestinationMessageDaoImpl destinationMessageDao;
    private final DestinationCreatedHookMessageGenerator destinationCreatedHookMessageGenerator;
    private final WebHookDao webHookDao;
    private final WebHookMessageSenderService webHookMessageSenderService;

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
            String partyId = account.getCreated().getPartyId();

            log.info("Start handling DestinationAccountCreatedChange: destinationId={}, partyId={}",
                    destinationId, partyId);

            DestinationMessage destinationMessage = destinationMessageDao.get(destinationId);

            webHookDao.getByPartyAndEventType(partyId, EventType.DESTINATION_CREATED)
                    .stream()
                    .map(webhook -> generateDestinationCreateHookMsg(
                            destinationMessage,
                            webhook,
                            destinationId,
                            event.getEventId(),
                            event.getCreatedAt()))
                    .forEach(webHookMessageSenderService::send);

            log.info("Finish handling DestinationAccountCreatedChange: destinationId={}, partyId={}",
                    destinationId, partyId);

        } catch (Exception e) {
            log.error("Error while handling DestinationAccountCreatedChange: {}", change, e);
            throw new HandleEventException("Error while handling DestinationAccountCreatedChange", e);
        }
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
}
