package dev.vality.wallets.hooker.handler.withdrawal;

import dev.vality.fistful.withdrawal.TimestampedChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.domain.enums.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalFailedHandler implements WithdrawalEventHandler {

    private final WithdrawalChangeStatusHandler withdrawalChangeStatusHandler;

    @Override
    public boolean accept(TimestampedChange change) {
        return change.getChange().isSetStatusChanged()
                && change.getChange().getStatusChanged().isSetStatus()
                && change.getChange().getStatusChanged().getStatus().isSetFailed();
    }

    @Override
    public void handle(TimestampedChange change, MachineEvent event) {
        String withdrawalId = event.getSourceId();
        log.info("Start handling WithdrawalFailedChange: withdrawalId={} change={}", withdrawalId, change);

        withdrawalChangeStatusHandler.handleChangeStatus(
                change,
                event,
                withdrawalId,
                EventType.WITHDRAWAL_FAILED);

        log.info("Finish handling WithdrawalFailedChange: withdrawalId={}", withdrawalId);
    }
}
