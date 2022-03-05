package dev.vality.wallets.hooker.handler.destination;

import dev.vality.fistful.destination.StatusChange;
import dev.vality.fistful.destination.TimestampedChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.dao.destination.DestinationReferenceDao;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.domain.tables.pojos.DestinationIdentityReference;
import dev.vality.wallets.hooker.handler.destination.generator.DestinationStatusChangeHookMessageGenerator;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DestinationAuthorizedHandler implements DestinationEventHandler {

    private final DestinationReferenceDao destinationReferenceDao;
    private final DestinationStatusChangeHookMessageGenerator destinationStatusChangeHookMessageGenerator;
    private final WebHookMessageSenderService webHookMessageSenderService;
    private final WebHookDao webHookDao;

    @Override
    public boolean accept(TimestampedChange change) {
        return change.getChange().isSetStatus()
                && change.getChange().getStatus().isSetChanged()
                && change.getChange().getStatus().getChanged().isSetAuthorized();
    }

    @Override
    public void handle(TimestampedChange change, MachineEvent event) {
        String destinationId = event.getSourceId();
        log.info("Start handling DestinationAuthorizedChange: destinationId={}", destinationId);

        DestinationIdentityReference destinationIdentityReference = destinationReferenceDao.get(destinationId);

        webHookDao.getByIdentityAndEventType(destinationIdentityReference.getIdentityId(),
                EventType.DESTINATION_AUTHORIZED)
                .stream()
                .map(webhook -> generateDestinationChangeHookMsg(
                        change.getChange().getStatus(),
                        webhook,
                        event.getSourceId(),
                        event.getEventId(),
                        Long.valueOf(destinationIdentityReference.getEventId()),
                        event.getCreatedAt(),
                        destinationIdentityReference.getExternalId()))
                .forEach(webHookMessageSenderService::send);

        log.info("Finish handling DestinationAuthorizedChange: destinationId={}", destinationId);
    }

    private WebhookMessage generateDestinationChangeHookMsg(
            StatusChange status,
            WebHookModel webhook,
            String sourcedId,
            long eventId,
            Long parentId,
            String createdAt,
            String externalId) {
        MessageGenParams messageGenParams = MessageGenParams.builder()
                .sourceId(sourcedId)
                .eventId(eventId)
                .parentId(parentId)
                .createdAt(createdAt)
                .externalId(externalId)
                .build();

        return destinationStatusChangeHookMessageGenerator.generate(status, webhook, messageGenParams);
    }
}
