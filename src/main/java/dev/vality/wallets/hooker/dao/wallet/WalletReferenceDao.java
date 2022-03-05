package dev.vality.wallets.hooker.dao.wallet;

import dev.vality.wallets.hooker.domain.tables.pojos.WalletIdentityReference;

public interface WalletReferenceDao {

    void create(WalletIdentityReference reference);

    WalletIdentityReference get(String id);

}
