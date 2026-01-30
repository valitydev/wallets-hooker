package dev.vality.wallets.hooker.service;

import dev.vality.fistful.base.EventRange;
import dev.vality.fistful.withdrawal.ManagementSrv;
import dev.vality.fistful.withdrawal.WithdrawalState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalClient {

    private final ManagementSrv.Iface withdrawalFistfulClient;
    private final RetryTemplate retryTemplate;

    public WithdrawalState getWithdrawalInfo(String withdrawalId, long eventId) {
        try {
            return retryTemplate.execute(context -> {
                log.debug("Attempt {} to fetch withdrawal state for withdrawalId={}, eventId={}", 
                        context.getRetryCount() + 1, withdrawalId, eventId);
                
                WithdrawalState withdrawalState = withdrawalFistfulClient.get(withdrawalId, createEventRange(eventId));
                if (withdrawalState == null) {
                    log.warn("Withdrawal not found for withdrawalId={}, eventId={} (attempt {})", 
                            withdrawalId, eventId, context.getRetryCount() + 1);
                    throw new RuntimeException("Withdrawal not found!");
                }
                
                log.debug("Successfully fetched withdrawal state for withdrawalId={} (attempt {})", 
                        withdrawalId, context.getRetryCount() + 1);
                return withdrawalState;
            });
        } catch (Exception e) {
            log.error("Error fetching withdrawal state for withdrawalId={}, eventId={}", withdrawalId, eventId, e);
            throw new RuntimeException("Failed to fetch withdrawal state", e);
        }
    }

    private EventRange createEventRange(long eventId) {
        return new EventRange().setLimit((int) eventId);
    }
}
