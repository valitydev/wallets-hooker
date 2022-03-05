package dev.vality.wallets.hooker.dao.destination;

import dev.vality.wallets.hooker.domain.tables.pojos.DestinationIdentityReference;

public interface DestinationReferenceDao {

    void create(DestinationIdentityReference reference);

    DestinationIdentityReference get(String id);

}
