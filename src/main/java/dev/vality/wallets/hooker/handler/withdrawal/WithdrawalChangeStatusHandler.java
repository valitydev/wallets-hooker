package dev.vality.wallets.hooker.handler.withdrawal;

import dev.vality.fistful.withdrawal.StatusChange;
import dev.vality.fistful.withdrawal.TimestampedChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.dao.withdrawal.WithdrawalReferenceDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.domain.tables.pojos.WithdrawalReference;
import dev.vality.wallets.hooker.exception.HandleEventException;
import dev.vality.wallets.hooker.handler.withdrawal.generator.WithdrawalStatusChangedHookMessageGenerator;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalChangeStatusHandler {

    @Value("${waiting.reference.period}")
    private int waitingPollPeriod;

    private final WithdrawalReferenceDao withdrawalReferenceDao;
    private final WebHookDao webHookDao;
    private final WithdrawalStatusChangedHookMessageGenerator withdrawalStatusChangedHookMessageGenerator;
    private final WebHookMessageSenderService webHookMessageSenderService;

    public void handleChangeStatus(
            TimestampedChange change,
            MachineEvent event,
            String withdrawalId,
            EventType eventType) {
        try {
            var reference = waitReferenceWithdrawal(withdrawalId);
            Long parentId = Long.valueOf(reference.getEventId());

            webHookDao.getByPartyAndEventType(reference.getPartyId(), eventType).stream()
                    .filter(webHook -> webHook.getWalletId() == null
                            || webHook.getWalletId().equals(reference.getWalletId()))
                    .map(webhook -> generateWithdrawalStatusChangeHookMsg(
                            change.getChange().getStatusChanged(),
                            webhook,
                            withdrawalId,
                            event.getEventId(),
                            parentId,
                            event.getCreatedAt(),
                            reference.getExternalId()))
                    .forEach(webHookMessageSenderService::send);
        } catch (Exception e) {
            log.error("Error while handling WithdrawalStatusChangedChange: {}, withdrawalId: {}",
                    change, withdrawalId, e);
            throw new HandleEventException("Error while handling WithdrawalStatusChangedChange", e);
        }
    }

    private WithdrawalReference waitReferenceWithdrawal(String withdrawalId) {
        var withdrawalReference = withdrawalReferenceDao.get(withdrawalId);
        while (withdrawalReference == null) {
            log.info("Waiting withdrawal create: '{}'", withdrawalId);
            try {
                Thread.sleep(waitingPollPeriod);
                withdrawalReference = withdrawalReferenceDao.get(withdrawalId);
            } catch (InterruptedException e) {
                log.error("Error when waiting withdrawal create: {} e: ", withdrawalId, e);
                Thread.currentThread().interrupt();
            }
        }

        return withdrawalReference;
    }

    private WebhookMessage generateWithdrawalStatusChangeHookMsg(
            StatusChange statusChanged,
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

        return withdrawalStatusChangedHookMessageGenerator.generate(statusChanged, webhook, messageGenParams);
    }

}
