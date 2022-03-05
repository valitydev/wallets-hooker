package dev.vality.wallets.hooker.dao.withdrawal;

import dev.vality.wallets.hooker.domain.tables.pojos.WithdrawalIdentityWalletReference;

public interface WithdrawalReferenceDao {

    void create(WithdrawalIdentityWalletReference reference);

    WithdrawalIdentityWalletReference get(String id);

}
