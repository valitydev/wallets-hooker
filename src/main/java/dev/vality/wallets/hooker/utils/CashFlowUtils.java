package dev.vality.wallets.hooker.utils;

import dev.vality.fistful.cashflow.FinalCashFlowPosting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CashFlowUtils {

    public static long getWithdrawalFee(List<FinalCashFlowPosting> postings) {
        return postings.stream()
                .filter(posting -> posting.getSource().getAccountType().isSetWallet()
                        && posting.getDestination().getAccountType().isSetSystem())
                .map(posting -> posting.getVolume().getAmount())
                .reduce(0L, Long::sum);
    }
}
