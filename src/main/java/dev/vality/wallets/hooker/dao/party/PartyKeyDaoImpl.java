package dev.vality.wallets.hooker.dao.party;

import dev.vality.mapper.RecordRowMapper;
import dev.vality.wallets.hooker.dao.AbstractDao;
import dev.vality.wallets.hooker.domain.tables.pojos.PartyKey;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import static dev.vality.wallets.hooker.domain.tables.PartyKey.PARTY_KEY;

@Component
public class PartyKeyDaoImpl extends AbstractDao implements PartyKeyDao {

    private final RowMapper<PartyKey> listRecordRowMapper;

    public PartyKeyDaoImpl(DataSource dataSource) {
        super(dataSource);
        this.listRecordRowMapper = new RecordRowMapper<>(PARTY_KEY, PartyKey.class);
    }

    @Override
    public void create(PartyKey partyKey) {
        var insertPartyKey = getDslContext()
                .insertInto(PARTY_KEY)
                .set(getDslContext()
                        .newRecord(PARTY_KEY, partyKey))
                .onConflict(PARTY_KEY.PARTY_ID)
                .doNothing();
        execute(insertPartyKey);
    }

    @Override
    public PartyKey get(Long id) {
        return fetchOne(getDslContext()
                        .select(PARTY_KEY.ID, PARTY_KEY.PARTY_ID, PARTY_KEY.PUB_KEY, PARTY_KEY.PRIV_KEY)
                        .from(PARTY_KEY)
                        .where(PARTY_KEY.ID.eq(id)),
                listRecordRowMapper);
    }

    @Override
    public PartyKey getByParty(String partyId) {
        return fetchOne(getDslContext()
                        .select(PARTY_KEY.ID, PARTY_KEY.PARTY_ID, PARTY_KEY.PUB_KEY, PARTY_KEY.PRIV_KEY)
                        .from(PARTY_KEY)
                        .where(PARTY_KEY.PARTY_ID.eq(partyId)),
                listRecordRowMapper);
    }
}
