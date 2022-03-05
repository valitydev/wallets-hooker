package dev.vality.wallets.hooker.dao.destination;

import dev.vality.wallets.hooker.domain.tables.pojos.DestinationMessage;

public interface DestinationMessageDao {

    void create(DestinationMessage reference);

    DestinationMessage get(String id);

}
