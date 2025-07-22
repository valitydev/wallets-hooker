package dev.vality.wallets.hooker.dao.party;

import dev.vality.wallets.hooker.domain.tables.pojos.PartyKey;

public interface PartyKeyDao {

    void create(PartyKey partyKey);

    PartyKey get(Long id);

    PartyKey getByParty(String partyId);

}
