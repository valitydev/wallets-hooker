package dev.vality.wallets.hooker.dao.withdrawal;

import dev.vality.wallets.hooker.domain.tables.pojos.WithdrawalReference;

public interface WithdrawalReferenceDao {

    void create(WithdrawalReference reference);

    WithdrawalReference get(String id);

}
