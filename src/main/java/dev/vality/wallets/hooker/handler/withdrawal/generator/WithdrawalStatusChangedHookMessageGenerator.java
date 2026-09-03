package dev.vality.wallets.hooker.handler.withdrawal.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.fistful.base.Cash;
import dev.vality.fistful.withdrawal.TimestampedChange;
import dev.vality.fistful.withdrawal.WithdrawalState;
import dev.vality.fistful.withdrawal.adjustment.BodyChangePlan;
import dev.vality.fistful.withdrawal.status.Status;
import dev.vality.swag.wallets.webhook.events.model.*;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.exception.GenerateMessageException;
import dev.vality.wallets.hooker.handler.AdditionalHeadersGenerator;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.wallets.hooker.service.BaseHookMessageGenerator;
import dev.vality.wallets.hooker.service.WebHookMessageGeneratorServiceImpl;
import dev.vality.wallets.hooker.utils.CashFlowUtils;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Slf4j
@Component
public class WithdrawalStatusChangedHookMessageGenerator extends BaseHookMessageGenerator<TimestampedChange> {

    private final WebHookMessageGeneratorServiceImpl<TimestampedChange> generatorService;
    private final ObjectMapper objectMapper;
    private final AdditionalHeadersGenerator additionalHeadersGenerator;

    public WithdrawalStatusChangedHookMessageGenerator(
            WebHookMessageGeneratorServiceImpl<TimestampedChange> generatorService,
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
            TimestampedChange event,
            WebHookModel model,
            MessageGenParams messageGenParams) {
        try {
            String message = initRequestBody(
                    event.getChange().isSetStatusChanged()
                            ? event.getChange().getStatusChanged().getStatus()
                            : messageGenParams.getWithdrawalState().getStatus(),
                    messageGenParams.getSourceId(),
                    messageGenParams.getEventId(),
                    messageGenParams.getCreatedAt(),
                    messageGenParams.getExternalId(),
                    messageGenParams.getWithdrawalState(),
                    event.getChange().isSetAdjustment() ? event.getChange().getAdjustment().getId() : null);

            WebhookMessage webhookMessage = generatorService.generate(event, model, messageGenParams);
            webhookMessage.setParentEventId(initParenId(model, messageGenParams.getParentId()));
            webhookMessage.setRequestBody(message.getBytes());
            webhookMessage.setAdditionalHeaders(additionalHeadersGenerator.generate(model, message));

            log.info(
                    "Webhook message from withdrawal_event_status_changed was generated, " +
                            "withdrawalId={}, statusChange={}, model={}, body={}, externalId={}",
                    messageGenParams.getSourceId(), event.toString(), model.toString(), message,
                    messageGenParams.getExternalId());

            return webhookMessage;
        } catch (Exception e) {
            log.error("Error when generate webhookMessage e: ", e);
            throw new GenerateMessageException("WithdrawalStatusChanged error when generate webhookMessage!", e);
        }

    }

    private Long initParenId(WebHookModel model, Long parentId) {
        if (model.getEventTypes() != null && model.getEventTypes().contains(EventType.WITHDRAWAL_CREATED)) {
            return parentId;
        }

        return super.parentIsNotExistId;
    }

    private String initRequestBody(
            Status status,
            String withdrawalId,
            Long eventId,
            String createdAt,
            String externalId,
            WithdrawalState withdrawalState,
            String adjustmentId) throws JsonProcessingException {
        if (status.isSetFailed()) {
            WithdrawalFailed withdrawalFailed = new WithdrawalFailed()
                    .withdrawalID(withdrawalId)
                    .externalID(externalId);
            withdrawalFailed.setEventType(Event.EventTypeEnum.WITHDRAWAL_FAILED);
            withdrawalFailed.setEventID(eventId.toString());
            withdrawalFailed.setOccuredAt(OffsetDateTime.parse(createdAt, DateTimeFormatter.ISO_DATE_TIME));
            withdrawalFailed.setTopic(Event.TopicEnum.WITHDRAWAL_TOPIC);
            return objectMapper.writeValueAsString(withdrawalFailed);
        } else if (status.isSetSucceeded()) {
            Fee fee = calculateFee(withdrawalId, eventId, withdrawalState);
            WithdrawalSucceeded withdrawalSucceeded = new WithdrawalSucceeded()
                    .withdrawalID(withdrawalId)
                    .externalID(externalId)
                    .fee(fee)
                    .body(initNewBody(withdrawalState, adjustmentId));
            withdrawalSucceeded.setEventType(Event.EventTypeEnum.WITHDRAWAL_SUCCEEDED);
            withdrawalSucceeded.setEventID(eventId.toString());
            withdrawalSucceeded.setOccuredAt(OffsetDateTime.parse(createdAt));
            withdrawalSucceeded.setTopic(Event.TopicEnum.WITHDRAWAL_TOPIC);
            return objectMapper.writeValueAsString(withdrawalSucceeded);
        } else {
            log.error("Unknown WithdrawalStatus status: {} withdrawalId: {}", status, withdrawalId);
            String message = String.format(
                    "Unknown WithdrawalStatus status: %s withdrawalId: %s",
                    status, withdrawalId);
            throw new GenerateMessageException(message);
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

    private WithdrawalBody initNewBody(WithdrawalState withdrawalState, String adjustmentId) {
        if (!amountChanged(withdrawalState)) {
            return null;
        }
        if (withdrawalState.isSetNewBody()) {
            return initBody(withdrawalState.getNewBody(), withdrawalState.getBody());
        }

        return withdrawalState.getAdjustments().stream()
                .filter(adjustmentState -> Objects.equals(adjustmentState.getId(), adjustmentId))
                .map(adjustment -> adjustment.getChangesPlan().getNewBody())
                .filter(Objects::nonNull)
                .map(BodyChangePlan::getNewBody)
                .findFirst()
                .map(newBody -> initBody(newBody, withdrawalState.getBody()))
                .orElse(null);
    }

    private boolean amountChanged(WithdrawalState withdrawalState) {
        return withdrawalState != null
                && (withdrawalState.isSetNewBody() || withdrawalState.isSetAdjustments());
    }

    private WithdrawalBody initBody(Cash newBody, Cash oldBody) {
        var withdrawalBody = new WithdrawalBody();
        withdrawalBody.setAmount(oldBody.getAmount());
        withdrawalBody.setChangedAmount(newBody.getAmount());
        withdrawalBody.setCurrency(newBody.getCurrency().getSymbolicCode());
        return withdrawalBody;
    }

}
