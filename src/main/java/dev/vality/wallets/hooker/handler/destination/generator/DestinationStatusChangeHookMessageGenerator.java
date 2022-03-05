package dev.vality.wallets.hooker.handler.destination.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.fistful.destination.StatusChange;
import dev.vality.swag.wallets.webhook.events.model.DestinationAuthorized;
import dev.vality.swag.wallets.webhook.events.model.DestinationUnauthorized;
import dev.vality.swag.wallets.webhook.events.model.Event;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.exception.GenerateMessageException;
import dev.vality.wallets.hooker.handler.AdditionalHeadersGenerator;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.wallets.hooker.service.WebHookMessageGeneratorServiceImpl;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.service.BaseHookMessageGenerator;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
public class DestinationStatusChangeHookMessageGenerator extends BaseHookMessageGenerator<StatusChange> {

    private final WebHookMessageGeneratorServiceImpl<StatusChange> generatorService;
    private final ObjectMapper objectMapper;
    private final AdditionalHeadersGenerator additionalHeadersGenerator;

    public DestinationStatusChangeHookMessageGenerator(
            WebHookMessageGeneratorServiceImpl<StatusChange> generatorService,
            ObjectMapper objectMapper, AdditionalHeadersGenerator additionalHeadersGenerator,
            @Value("${parent.not.exist.id}") Long parentId) {
        super(parentId);
        this.generatorService = generatorService;
        this.objectMapper = objectMapper;
        this.additionalHeadersGenerator = additionalHeadersGenerator;
    }

    @Override
    protected WebhookMessage generateMessage(
            StatusChange statusChange,
            WebHookModel model,
            MessageGenParams messageGenParams) {
        try {
            String message = generateMessage(
                    statusChange,
                    messageGenParams.getSourceId(),
                    messageGenParams.getEventId(),
                    messageGenParams.getCreatedAt(),
                    messageGenParams.getExternalId());

            Map<String, String> additionalHeaders = additionalHeadersGenerator.generate(model, message);

            WebhookMessage webhookMessage = generatorService.generate(statusChange, model, messageGenParams);
            webhookMessage.setParentEventId(initParentId(model, messageGenParams.getParentId()));
            webhookMessage.setAdditionalHeaders(additionalHeaders);
            webhookMessage.setRequestBody(message.getBytes());

            log.info(
                    "Webhook message from destination_event_status_changed was generated, " +
                            "destinationId={}, statusChange={}, model={}",
                    messageGenParams.getSourceId(), statusChange.toString(), model.toString());

            return webhookMessage;
        } catch (Exception e) {
            log.error("Error when generate webhookMessage e: ", e);
            throw new GenerateMessageException("Error when generate webhookMessage", e);
        }

    }

    private String generateMessage(
            StatusChange statusChange,
            String destinationId,
            Long eventId,
            String createdAt,
            String externalId) throws JsonProcessingException {

        if (statusChange.getChanged().isSetAuthorized()) {
            DestinationAuthorized destination = new DestinationAuthorized();
            destination.setDestinationID(destinationId);
            destination.setEventID(eventId.toString());
            destination.setEventType(Event.EventTypeEnum.DESTINATIONAUTHORIZED);
            destination.setOccuredAt(OffsetDateTime.parse(createdAt, DateTimeFormatter.ISO_DATE_TIME));
            destination.setTopic(Event.TopicEnum.DESTINATIONTOPIC);
            destination.setExternalID(externalId);
            return objectMapper.writeValueAsString(destination);
        } else if (statusChange.getChanged().isSetUnauthorized()) {
            DestinationUnauthorized destination = new DestinationUnauthorized();
            destination.setDestinationID(destinationId);
            destination.setEventID(eventId.toString());
            destination.setEventType(Event.EventTypeEnum.DESTINATIONUNAUTHORIZED);
            destination.setOccuredAt(OffsetDateTime.parse(createdAt, DateTimeFormatter.ISO_DATE_TIME));
            destination.setTopic(Event.TopicEnum.DESTINATIONTOPIC);
            destination.setExternalID(externalId);
            return objectMapper.writeValueAsString(destination);
        } else {
            log.error("Unknown statusChange: {}", statusChange);
            throw new GenerateMessageException("Unknown statusChange!");
        }
    }

    private Long initParentId(WebHookModel model, Long parentId) {
        if (model.getEventTypes() != null && model.getEventTypes().contains(EventType.DESTINATION_CREATED)) {
            return parentId;
        }

        return parentIsNotExistId;
    }
}
