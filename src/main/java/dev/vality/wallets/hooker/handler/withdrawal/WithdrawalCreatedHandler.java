package dev.vality.wallets.hooker.handler.withdrawal;

import dev.vality.fistful.withdrawal.TimestampedChange;
import dev.vality.fistful.withdrawal.Withdrawal;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.dao.destination.DestinationReferenceDao;
import dev.vality.wallets.hooker.dao.wallet.WalletReferenceDao;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.dao.withdrawal.WithdrawalReferenceDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.domain.tables.pojos.DestinationIdentityReference;
import dev.vality.wallets.hooker.domain.tables.pojos.WalletIdentityReference;
import dev.vality.wallets.hooker.domain.tables.pojos.WithdrawalIdentityWalletReference;
import dev.vality.wallets.hooker.exception.HandleEventException;
import dev.vality.wallets.hooker.handler.withdrawal.generator.WithdrawalCreatedHookMessageGenerator;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalCreatedHandler implements WithdrawalEventHandler {

    @Value("${waiting.reference.period}")
    private int waitingPollPeriod;

    private final WithdrawalReferenceDao withdrawalReferenceDao;
    private final DestinationReferenceDao destinationReferenceDao;
    private final WalletReferenceDao walletReferenceDao;
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

            log.info("Start handling WithdrawalCreatedChange: destinationId={}, withdrawal={}, walletId={}",
                    destinationId, withdrawal, walletId);

            DestinationIdentityReference destinationIdentityReference = destinationReferenceDao.get(destinationId);
            WalletIdentityReference walletIdentityReference = walletReferenceDao.get(walletId);

            while (destinationIdentityReference == null || walletIdentityReference == null) {
                log.info("Waiting destination: {} or wallet: {} !", destinationId, walletId);
                try {
                    Thread.sleep(waitingPollPeriod);
                    destinationIdentityReference = destinationReferenceDao.get(destinationId);
                    walletIdentityReference = walletReferenceDao.get(walletId);
                } catch (InterruptedException e) {
                    log.error("Error when waiting destination: {} or wallet: {} e: ", destinationId, walletId, e);
                    Thread.currentThread().interrupt();
                }
            }

            createReference(
                    withdrawal,
                    destinationIdentityReference,
                    String.valueOf(eventId),
                    withdrawalId);

            findWebhookModels(destinationIdentityReference, walletIdentityReference)
                    .stream()
                    .filter(webhook -> webhook.getWalletId() == null || webhook.getWalletId().equals(walletId))
                    .map(webhook -> generateWithdrawalCreatedHookMsg(
                            withdrawal,
                            webhook,
                            withdrawalId,
                            eventId,
                            event.getCreatedAt(),
                            withdrawal.getExternalId()))
                    .forEach(webHookMessageSenderService::send);

            log.info("Finish handling WithdrawalCreatedChange: destinationId={}, withdrawalId={}, walletId={}",
                    destinationId, withdrawalId, walletId);
        } catch (Exception e) {
            log.error("Error while handling WithdrawalCreatedChange: {}, event: {}", change, event, e);
            throw new HandleEventException("Error while handling WithdrawalCreatedChange", e);
        }
    }

    private List<WebHookModel> findWebhookModels(
            DestinationIdentityReference destinationIdentityReference,
            WalletIdentityReference walletIdentityReference) {
        List<WebHookModel> webHookModels = webHookDao.getByIdentityAndEventType(
                destinationIdentityReference.getIdentityId(),
                EventType.WITHDRAWAL_CREATED);

        if (!destinationIdentityReference.getIdentityId().equals(walletIdentityReference.getIdentityId())) {
            List<WebHookModel> webHookModelsWallets = webHookDao.getByIdentityAndEventType(
                    walletIdentityReference.getIdentityId(),
                    EventType.WITHDRAWAL_CREATED);
            webHookModels.addAll(webHookModelsWallets);
        }

        return webHookModels;
    }

    private void createReference(
            Withdrawal withdrawal,
            DestinationIdentityReference destinationIdentityReference,
            String eventId,
            String withdrawalId) {
        WithdrawalIdentityWalletReference withdrawalIdentityWalletReference = new WithdrawalIdentityWalletReference();
        withdrawalIdentityWalletReference.setIdentityId(destinationIdentityReference.getIdentityId());
        withdrawalIdentityWalletReference.setWalletId(withdrawal.getWalletId());
        withdrawalIdentityWalletReference.setWithdrawalId(withdrawalId);
        withdrawalIdentityWalletReference.setEventId(eventId);
        withdrawalIdentityWalletReference.setExternalId(withdrawal.getExternalId());

        withdrawalReferenceDao.create(withdrawalIdentityWalletReference);
    }

    private WebhookMessage generateWithdrawalCreatedHookMsg(
            Withdrawal withdrawal,
            WebHookModel webhook,
            String withdrawalId,
            long eventId,
            String createdAt,
            String externalId) {
        MessageGenParams msgGenParams = MessageGenParams.builder()
                .sourceId(withdrawalId)
                .eventId(eventId)
                .createdAt(createdAt)
                .externalId(externalId)
                .build();

        return withdrawalCreatedHookMessageGenerator.generate(withdrawal, webhook, msgGenParams);
    }
}
