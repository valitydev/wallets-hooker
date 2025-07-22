package dev.vality.wallets.hooker.handler.withdrawal;

import dev.vality.fistful.withdrawal.TimestampedChange;
import dev.vality.fistful.withdrawal.Withdrawal;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.dao.withdrawal.WithdrawalReferenceDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.domain.tables.pojos.WithdrawalReference;
import dev.vality.wallets.hooker.exception.HandleEventException;
import dev.vality.wallets.hooker.handler.withdrawal.generator.WithdrawalCreatedHookMessageGenerator;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalCreatedHandler implements WithdrawalEventHandler {

    private final WithdrawalReferenceDao withdrawalReferenceDao;
    private final WebHookDao webHookDao;
    private final WithdrawalCreatedHookMessageGenerator withdrawalCreatedHookMessageGenerator;
    private final WebHookMessageSenderService webHookMessageSenderService;

    @Override
    public boolean accept(TimestampedChange change) {
        return change.getChange().isSetCreated();
    }

    @Override
    public void handle(TimestampedChange change, MachineEvent event) {
        try {
            long eventId = event.getEventId();
            Withdrawal withdrawal = change.getChange().getCreated().getWithdrawal();
            String withdrawalId = event.getSourceId();
            String destinationId = withdrawal.getDestinationId();
            String walletId = withdrawal.getWalletId();
            String partyId = withdrawal.getPartyId();

            log.info("Start handling WithdrawalCreatedChange: destinationId={}, withdrawal={}, walletId={}, partyId={}",
                    destinationId, withdrawal, walletId, partyId);
            createReference(
                    withdrawal,
                    String.valueOf(eventId),
                    withdrawalId);

            List<WebHookModel> webHookModels = webHookDao.getByPartyAndEventType(
                    partyId,
                    EventType.WITHDRAWAL_CREATED);

            MessageGenParams msgGenParams = MessageGenParams.builder()
                    .sourceId(withdrawalId)
                    .eventId(eventId)
                    .createdAt(event.getCreatedAt())
                    .externalId(withdrawal.getExternalId())
                    .build();

            webHookModels
                    .stream()
                    .filter(webhook -> webhook.getWalletId() == null || webhook.getWalletId().equals(walletId))
                    .map(webhook -> generateWithdrawalCreatedHookMsg(withdrawal, webhook, msgGenParams))
                    .forEach(webHookMessageSenderService::send);

            log.info("Finish handling WithdrawalCreatedChange: destinationId={}, withdrawalId={}, walletId={}",
                    destinationId, withdrawalId, walletId);
        } catch (Exception e) {
            log.error("Error while handling WithdrawalCreatedChange: {}, event: {}", change, event, e);
            throw new HandleEventException("Error while handling WithdrawalCreatedChange", e);
        }
    }

    private void createReference(
            Withdrawal withdrawal,
            String eventId,
            String withdrawalId) {
        WithdrawalReference withdrawalReference = new WithdrawalReference();
        withdrawalReference.setPartyId(withdrawal.getPartyId());
        withdrawalReference.setWalletId(withdrawal.getWalletId());
        withdrawalReference.setWithdrawalId(withdrawalId);
        withdrawalReference.setEventId(eventId);
        withdrawalReference.setExternalId(withdrawal.getExternalId());

        withdrawalReferenceDao.create(withdrawalReference);
    }

    private WebhookMessage generateWithdrawalCreatedHookMsg(
            Withdrawal withdrawal,
            WebHookModel webhook,
            MessageGenParams messageGenParams) {
        return withdrawalCreatedHookMessageGenerator.generate(withdrawal, webhook, messageGenParams);
    }
}
