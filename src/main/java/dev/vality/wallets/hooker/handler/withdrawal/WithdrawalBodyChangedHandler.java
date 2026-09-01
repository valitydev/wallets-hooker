package dev.vality.wallets.hooker.handler.withdrawal;

import dev.vality.fistful.withdrawal.BodyChange;
import dev.vality.fistful.withdrawal.TimestampedChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.dao.withdrawal.WithdrawalReferenceDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.exception.HandleEventException;
import dev.vality.wallets.hooker.handler.withdrawal.generator.WithdrawalBodyChangedHookMessageGenerator;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalBodyChangedHandler implements WithdrawalEventHandler {

    private final WithdrawalReferenceDao withdrawalReferenceDao;
    private final WebHookDao webHookDao;
    private final WithdrawalBodyChangedHookMessageGenerator withdrawalBodyChangedHookMessageGenerator;
    private final WebHookMessageSenderService webHookMessageSenderService;

    @Override
    public boolean accept(TimestampedChange change) {
        return change.getChange().isSetBodyChanged();
    }

    @Override
    public void handle(TimestampedChange change, MachineEvent event) {
        String withdrawalId = event.getSourceId();
        log.info("Start handling WithdrawalBodyChange: withdrawalId={} change={}", withdrawalId, change);

        handleChangeBody(
                change,
                event,
                withdrawalId,
                EventType.WITHDRAWAL_CASH_CHANGED);

        log.info("Finish handling WithdrawalBodyChange: withdrawalId={}", withdrawalId);
    }

    public void handleChangeBody(
            TimestampedChange change,
            MachineEvent event,
            String withdrawalId,
            EventType eventType) {
        try {
            var reference = withdrawalReferenceDao.get(withdrawalId);
            ;
            Long parentId = Long.valueOf(reference.getEventId());

            webHookDao.getByPartyAndEventType(reference.getPartyId(), eventType).stream()
                    .filter(webHook -> webHook.getWalletId() == null
                            || webHook.getWalletId().equals(reference.getWalletId()))
                    .map(webhook -> generateWithdrawalBodyChangeHookMsg(
                            change.getChange().getBodyChanged(),
                            webhook,
                            withdrawalId,
                            event.getEventId(),
                            parentId,
                            event.getCreatedAt(),
                            reference.getExternalId()))
                    .forEach(webHookMessageSenderService::send);
        } catch (Exception e) {
            log.error("Error while handling WithdrawalBodyChange: {}, withdrawalId: {}",
                    change, withdrawalId, e);
            throw new HandleEventException("Error while handling WithdrawalBodyChange", e);
        }
    }

    private WebhookMessage generateWithdrawalBodyChangeHookMsg(
            BodyChange bodyChanged,
            WebHookModel webhook,
            String withdrawalId,
            long eventId,
            Long parentId,
            String createdAt,
            String externalId) {
        MessageGenParams messageGenParams = MessageGenParams.builder()
                .sourceId(withdrawalId)
                .eventId(eventId)
                .parentId(parentId)
                .createdAt(createdAt)
                .externalId(externalId)
                .build();

        return withdrawalBodyChangedHookMessageGenerator.generate(bodyChanged, webhook, messageGenParams);
    }
}
