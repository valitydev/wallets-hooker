package dev.vality.wallets.hooker.dao.identity;

import dev.vality.wallets.hooker.domain.tables.pojos.IdentityKey;

public interface IdentityKeyDao {

    void create(IdentityKey identityKey);

    IdentityKey get(Long id);

    IdentityKey getByIdentity(String identityId);

}
