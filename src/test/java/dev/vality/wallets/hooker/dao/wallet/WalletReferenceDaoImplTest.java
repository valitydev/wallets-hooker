package dev.vality.wallets.hooker.dao.wallet;

import dev.vality.wallets.hooker.config.PostgresqlSpringBootITest;
import dev.vality.wallets.hooker.domain.tables.pojos.WalletIdentityReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@PostgresqlSpringBootITest
class WalletReferenceDaoImplTest {

    @Autowired
    private WalletReferenceDao walletReferenceDao;

    @Test
    void create() {
        WalletIdentityReference reference = new WalletIdentityReference();
        reference.setIdentityId("identity");
        String walletId = "walletId";
        reference.setWalletId(walletId);
        walletReferenceDao.create(reference);

        WalletIdentityReference walletIdentityReference = walletReferenceDao.get(walletId);

        assertEquals(reference, walletIdentityReference);
    }
}