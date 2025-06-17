package dev.vality.wallets.hooker.handler.destination.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vality.swag.wallets.webhook.events.model.DestinationCreated;
import dev.vality.swag.wallets.webhook.events.model.Event;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.tables.pojos.DestinationMessage;
import dev.vality.wallets.hooker.exception.GenerateMessageException;
import dev.vality.wallets.hooker.handler.AdditionalHeadersGenerator;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.wallets.hooker.service.BaseHookMessageGenerator;
import dev.vality.wallets.hooker.service.WebHookMessageGeneratorServiceImpl;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class DestinationCreatedHookMessageGenerator extends BaseHookMessageGenerator<DestinationMessage> {

    public static final String DESTINATION = "destination";
    public static final String PARTY = "party";
    private final WebHookMessageGeneratorServiceImpl<DestinationMessage> generatorService;
    private final ObjectMapper objectMapper;
    private final AdditionalHeadersGenerator additionalHeadersGenerator;

    public DestinationCreatedHookMessageGenerator(
            WebHookMessageGeneratorServiceImpl<DestinationMessage> generatorService,
            ObjectMapper objectMapper,
            AdditionalHeadersGenerator additionalHeadersGenerator,
            @Value("${parent.not.exist.id}") Long parentId) {
        super(parentId);
        this.generatorService = generatorService;
        this.objectMapper = objectMapper;
        this.additionalHeadersGenerator = additionalHeadersGenerator;
    }

    @Override
    protected WebhookMessage generateMessage(
            DestinationMessage event,
            WebHookModel model,
            MessageGenParams messageGenParams) {
        try {
            DestinationCreated destinationCreated = buildDestinationCreated(messageGenParams);
            String requestBody = objectMapper.writeValueAsString(destinationCreated);

            String messageString = initResultMessage(event, model, requestBody);
            WebhookMessage webhookMessage = generatorService.generate(event, model, messageGenParams);

            webhookMessage.setRequestBody(messageString.getBytes());
            webhookMessage.setAdditionalHeaders(additionalHeadersGenerator.generate(model, messageString));
            webhookMessage.setEventId(messageGenParams.getEventId());

            log.info("Webhook message from destination_event_created was generated, destinationId={}, model={}",
                    messageGenParams.getSourceId(), model);

            return webhookMessage;
        } catch (Exception e) {
            log.error("DestinationCreatedHookMessageGenerator error when generate destinationMessage: {} model: {} e: ",
                    event, model.toString(), e);
            throw new GenerateMessageException(
                    "DestinationCreatedHookMessageGenerator error when generate destinationMessage!", e);
        }

    }

    private DestinationCreated buildDestinationCreated(MessageGenParams messageGenParams) {
        DestinationCreated destinationCreated = new DestinationCreated();
        destinationCreated.setEventID(messageGenParams.getEventId().toString());
        destinationCreated.setEventType(Event.EventTypeEnum.DESTINATION_CREATED);
        OffsetDateTime parse = OffsetDateTime.parse(
                messageGenParams.getCreatedAt(),
                DateTimeFormatter.ISO_DATE_TIME);
        destinationCreated.setOccuredAt(parse);
        destinationCreated.setTopic(Event.TopicEnum.DESTINATION_TOPIC);
        return destinationCreated;
    }

    private String initResultMessage(DestinationMessage event, WebHookModel model, String requestBody)
            throws JsonProcessingException {
        JsonNode jsonNodeRoot = objectMapper.readTree(event.getMessage());
        JsonNode resultDestinationNode = ((ObjectNode) jsonNodeRoot).put(PARTY, model.getPartyId());
        JsonNode requestBodyJson = objectMapper.readTree(requestBody);
        JsonNode messageResult = ((ObjectNode) requestBodyJson).set(DESTINATION, resultDestinationNode);
        return objectMapper.writeValueAsString(messageResult);
    }

}
