package dev.vality.wallets.hooker.handler.withdrawal.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.fistful.withdrawal.BodyChange;
import dev.vality.fistful.withdrawal.WithdrawalState;
import dev.vality.swag.wallets.webhook.events.model.Event;
import dev.vality.swag.wallets.webhook.events.model.Fee;
import dev.vality.swag.wallets.webhook.events.model.WithdrawalBody;
import dev.vality.swag.wallets.webhook.events.model.WithdrawalCashChanged;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.exception.GenerateMessageException;
import dev.vality.wallets.hooker.handler.AdditionalHeadersGenerator;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.wallets.hooker.service.BaseHookMessageGenerator;
import dev.vality.wallets.hooker.service.WebHookMessageGeneratorServiceImpl;
import dev.vality.wallets.hooker.service.WithdrawalClient;
import dev.vality.wallets.hooker.utils.CashFlowUtils;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Slf4j
@Component
public class WithdrawalBodyChangedHookMessageGenerator extends BaseHookMessageGenerator<BodyChange> {

    private final WebHookMessageGeneratorServiceImpl<BodyChange> generatorService;
    private final ObjectMapper objectMapper;
    private final AdditionalHeadersGenerator additionalHeadersGenerator;
    private final WithdrawalClient withdrawalClient;

    public WithdrawalBodyChangedHookMessageGenerator(
            WebHookMessageGeneratorServiceImpl<BodyChange> generatorService,
            ObjectMapper objectMapper,
            AdditionalHeadersGenerator additionalHeadersGenerator,
            WithdrawalClient withdrawalClient,
            @Value("${parent.not.exist.id}") Long parentId) {
        super(parentId);
        this.generatorService = generatorService;
        this.objectMapper = objectMapper;
        this.additionalHeadersGenerator = additionalHeadersGenerator;
        this.withdrawalClient = withdrawalClient;
    }

    @Override
    protected WebhookMessage generateMessage(
            BodyChange event,
            WebHookModel model,
            MessageGenParams messageGenParams) {
        try {
            var withdrawalId = messageGenParams.getSourceId();
            var eventId = messageGenParams.getEventId();
            WithdrawalState withdrawalState = getWithdrawalState(withdrawalId, eventId);
            Fee fee = calculateFee(withdrawalId, eventId, withdrawalState);
            WithdrawalCashChanged withdrawalCashChanged = new WithdrawalCashChanged()
                    .withdrawalID(withdrawalId)
                    .externalID(messageGenParams.getExternalId())
                    .fee(fee)
                    .body(new WithdrawalBody()
                            .amount(event.getOldBody().getAmount())
                            .changedAmount(event.getNewBody().getAmount())
                            .currency(event.getNewBody().getCurrency().getSymbolicCode()));
            withdrawalCashChanged.setEventType(Event.EventTypeEnum.WITHDRAWAL_CASH_CHANGED);
            withdrawalCashChanged.setEventID(eventId.toString());
            withdrawalCashChanged.setOccuredAt(OffsetDateTime.parse(messageGenParams.getCreatedAt()));
            withdrawalCashChanged.setTopic(Event.TopicEnum.WITHDRAWAL_TOPIC);
            String message = objectMapper.writeValueAsString(withdrawalCashChanged);

            WebhookMessage webhookMessage = generatorService.generate(event, model, messageGenParams);
            webhookMessage.setParentEventId(initPatenId(model, messageGenParams.getParentId()));
            webhookMessage.setRequestBody(message.getBytes());
            webhookMessage.setAdditionalHeaders(additionalHeadersGenerator.generate(model, message));

            log.info(
                    "Webhook message from withdrawal_event_body_changed was generated, " +
                            "withdrawalId={}, bodyChange={}, model={}, body={}, externalId={}",
                    messageGenParams.getSourceId(), event, model, message,
                    messageGenParams.getExternalId());

            return webhookMessage;
        } catch (JsonProcessingException e) {
            log.error("Error when generate WithdrawalCashChanged event: {} model: {} eventId: {} e: ",
                    event, model, messageGenParams.getEventId(), e);
            throw new GenerateMessageException("WithdrawalCreated error when generate webhookMessage!", e);
        }

    }

    private Long initPatenId(WebHookModel model, Long parentId) {
        if (model.getEventTypes() != null && model.getEventTypes().contains(EventType.WITHDRAWAL_CREATED)) {
            return parentId;
        }

        return super.parentIsNotExistId;
    }

    private WithdrawalState getWithdrawalState(String withdrawalId, Long eventId) {
        try {
            return withdrawalClient.getWithdrawalInfo(withdrawalId, eventId);
        } catch (Exception e) {
            log.warn("Error getting withdrawal state for withdrawalId={}, eventId={}: {}",
                    withdrawalId, eventId, e.getMessage());
            return null;
        }
    }

    private Fee calculateFee(String withdrawalId, Long eventId, WithdrawalState withdrawalState) {
        if (withdrawalState != null
                && withdrawalState.getEffectiveFinalCashFlow() != null
                && withdrawalState.getEffectiveFinalCashFlow().getPostings() != null) {
            long amount = CashFlowUtils.getWithdrawalFee(withdrawalState.getEffectiveFinalCashFlow().getPostings());
            String currency = withdrawalState.getBody().getCurrency().getSymbolicCode();
            return new Fee().amount(amount).currency(currency);
        }
        log.warn("Unable to calculate fee for withdrawalId={}, eventId={}: missing cash flow data",
                withdrawalId, eventId);
        return null;
    }

}
