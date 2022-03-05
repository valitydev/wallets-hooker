package dev.vality.wallets.hooker.handler.wallet;

import dev.vality.fistful.wallet.TimestampedChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.dao.wallet.WalletReferenceDao;
import dev.vality.wallets.hooker.domain.tables.pojos.WalletIdentityReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletAccountCreatedHandler implements WalletEventHandler {

    private final WalletReferenceDao walletReferenceDao;

    @Override
    public boolean accept(TimestampedChange change) {
        return change.getChange().isSetAccount()
                && change.getChange().getAccount().isSetCreated();
    }

    @Override
    public void handle(TimestampedChange change, MachineEvent event) {
        String walletId = event.getSourceId();
        String identityId = change.getChange().getAccount().getCreated().getIdentity();

        log.info("Start handling WalletAccountCreatedChange: walletId={}, identityId={}", walletId, identityId);

        WalletIdentityReference reference = new WalletIdentityReference();
        reference.setWalletId(walletId);
        reference.setIdentityId(identityId);

        walletReferenceDao.create(reference);

        log.info("Finish handling WalletAccountCreatedChange: walletId={}, identityId={}", walletId, identityId);
    }
}
