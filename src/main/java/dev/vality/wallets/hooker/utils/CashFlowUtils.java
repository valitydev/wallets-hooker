package dev.vality.wallets.hooker.utils;

import dev.vality.fistful.cashflow.FinalCashFlowPosting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CashFlowUtils {

    public static long getFistfulFee(List<FinalCashFlowPosting> postings) {
        return getFistfulAmount(
                postings,
                posting -> posting.getSource().getAccountType().isSetWallet()
                        && posting.getDestination().getAccountType().isSetSystem()
        );
    }

    public static long getFistfulAmount(
            List<dev.vality.fistful.cashflow.FinalCashFlowPosting> postings,
            Predicate<FinalCashFlowPosting> filter
    ) {
        return postings.stream()
                .filter(filter)
                .map(posting -> posting.getVolume().getAmount())
                .reduce(0L, Long::sum);
    }
}
