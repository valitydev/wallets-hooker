package dev.vality.wallets.hooker.service;

import dev.vality.fistful.base.EventRange;
import dev.vality.fistful.withdrawal.ManagementSrv;
import dev.vality.fistful.withdrawal.WithdrawalState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalClient {

    private final ManagementSrv.Iface withdrawalFistfulClient;

    public WithdrawalState getWithdrawalInfo(String withdrawalId, long eventId) {
        try {
            log.debug("Fetching withdrawal state for withdrawalId={}, eventId={}", withdrawalId, eventId);
            WithdrawalState withdrawalState = withdrawalFistfulClient.get(withdrawalId, createEventRange(eventId));
            if (withdrawalState == null) {
                log.warn("Withdrawal not found for withdrawalId={}, eventId={}", withdrawalId, eventId);
                throw new RuntimeException("Withdrawal not found!");
            }
            log.debug("Successfully fetched withdrawal state for withdrawalId={}", withdrawalId);
            return withdrawalState;
        } catch (Exception e) {
            log.error("Error fetching withdrawal state for withdrawalId={}, eventId={}", withdrawalId, eventId, e);
            throw new RuntimeException("Failed to fetch withdrawal state", e);
        }
    }

    private EventRange createEventRange(long eventId) {
        return new EventRange().setLimit((int) eventId);
    }
}
