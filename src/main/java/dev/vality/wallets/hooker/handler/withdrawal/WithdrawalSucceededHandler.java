package dev.vality.wallets.hooker.handler.withdrawal;

import dev.vality.fistful.withdrawal.TimestampedChange;
import dev.vality.fistful.withdrawal.WithdrawalState;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.service.WithdrawalClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalSucceededHandler implements WithdrawalEventHandler {

    private final WithdrawalChangeStatusHandler withdrawalChangeStatusHandler;
    private final WithdrawalClient withdrawalClient;

    @Override
    public boolean accept(TimestampedChange change) {
        return change.getChange().isSetStatusChanged()
                && change.getChange().getStatusChanged().isSetStatus()
                && change.getChange().getStatusChanged().getStatus().isSetSucceeded();
    }

    @Override
    public void handle(TimestampedChange change, MachineEvent event) {
        String withdrawalId = event.getSourceId();
        WithdrawalState withdrawalState = getWithdrawalState(withdrawalId, event.getEventId());

        log.info("Start handling WithdrawalSucceededChange: withdrawalId={} change={}", withdrawalId, change);

        withdrawalChangeStatusHandler.handleChangeStatus(
                change,
                event,
                withdrawalId,
                EventType.WITHDRAWAL_SUCCEEDED,
                withdrawalState);

        log.info("Finish handling WithdrawalSucceededChange: withdrawalId={}", withdrawalId);
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
}