package dev.vality.wallets.hooker.handler.withdrawal.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.fistful.base.Cash;
import dev.vality.fistful.withdrawal.Withdrawal;
import dev.vality.swag.wallets.webhook.events.model.Event;
import dev.vality.swag.wallets.webhook.events.model.WithdrawalBody;
import dev.vality.swag.wallets.webhook.events.model.WithdrawalStarted;
import dev.vality.wallets.hooker.domain.WebHookModel;
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
public class WithdrawalCreatedHookMessageGenerator extends BaseHookMessageGenerator<Withdrawal> {

    private final WebHookMessageGeneratorServiceImpl<Withdrawal> generatorService;
    private final ObjectMapper objectMapper;
    private final AdditionalHeadersGenerator additionalHeadersGenerator;

    public WithdrawalCreatedHookMessageGenerator(
            WebHookMessageGeneratorServiceImpl<Withdrawal> generatorService,
            ObjectMapper objectMapper,
            AdditionalHeadersGenerator additionalHeadersGenerator,
            @Value("${parent.not.exist.id}") Long parentId) {
        super(parentId);
        this.generatorService = generatorService;
        this.objectMapper = objectMapper;
        this.additionalHeadersGenerator = additionalHeadersGenerator;
    }

    @Override
    protected WebhookMessage generateMessage(Withdrawal event, WebHookModel model, MessageGenParams messageGenParams) {
        try {
            var withdrawal = new dev.vality.swag.wallets.webhook.events.model.Withdrawal();
            withdrawal.setDestination(event.getDestinationId());
            withdrawal.setId(messageGenParams.getSourceId());
            withdrawal.setWallet(event.getWalletId());
            withdrawal.setBody(initBody(event));
            withdrawal.setExternalID(event.getExternalId());
            WithdrawalStarted withdrawalStarted = new WithdrawalStarted();
            withdrawalStarted.setWithdrawal(withdrawal);
            withdrawalStarted.setEventType(Event.EventTypeEnum.WITHDRAWALSTARTED);
            withdrawalStarted.setEventID(messageGenParams.getEventId().toString());
            withdrawalStarted.setOccuredAt(OffsetDateTime.parse(
                    messageGenParams.getCreatedAt(),
                    DateTimeFormatter.ISO_DATE_TIME));
            withdrawalStarted.setTopic(Event.TopicEnum.WITHDRAWALTOPIC);

            String requestBody = objectMapper.writeValueAsString(withdrawalStarted);

            WebhookMessage webhookMessage = generatorService.generate(event, model, messageGenParams);
            webhookMessage.setRequestBody(requestBody.getBytes());
            webhookMessage.setAdditionalHeaders(additionalHeadersGenerator.generate(model, requestBody));
            webhookMessage.setParentEventId(messageGenParams.getParentId());

            log.info(
                    "Webhook message from withdrawal_event_created was generated, " +
                            "withdrawalStarted={}, model={}, requestBody={}",
                    messageGenParams.getSourceId(), model.toString(), requestBody);

            return webhookMessage;
        } catch (JsonProcessingException e) {
            log.error("Error when generate webhookMessage event: {} model: {} eventId: {} e: ",
                    event, model, messageGenParams.getEventId(), e);
            throw new GenerateMessageException("WithdrawalCreated error when generate webhookMessage!", e);
        }

    }

    private WithdrawalBody initBody(Withdrawal event) {
        Cash body = event.getBody();
        var withdrawalBody = new WithdrawalBody();
        withdrawalBody.setAmount(body.getAmount());
        withdrawalBody.setCurrency(body.getCurrency().getSymbolicCode());
        return withdrawalBody;
    }
}
