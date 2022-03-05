package dev.vality.wallets.hooker.dao.wallet;

import dev.vality.mapper.RecordRowMapper;
import dev.vality.wallets.hooker.dao.AbstractDao;
import dev.vality.wallets.hooker.domain.tables.pojos.WalletIdentityReference;
import dev.vality.wallets.hooker.domain.tables.records.WalletIdentityReferenceRecord;
import lombok.extern.slf4j.Slf4j;
import org.jooq.InsertReturningStep;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import static dev.vality.wallets.hooker.domain.tables.WalletIdentityReference.WALLET_IDENTITY_REFERENCE;

@Component
@Slf4j
public class WalletReferenceDaoImpl extends AbstractDao implements WalletReferenceDao {

    private final RowMapper<WalletIdentityReference> listRecordRowMapper;

    public WalletReferenceDaoImpl(DataSource dataSource) {
        super(dataSource);
        this.listRecordRowMapper = new RecordRowMapper<>(WALLET_IDENTITY_REFERENCE, WalletIdentityReference.class);
    }

    @Override
    public void create(WalletIdentityReference reference) {
        InsertReturningStep<WalletIdentityReferenceRecord> insertReturningStep = getDslContext()
                .insertInto(WALLET_IDENTITY_REFERENCE)
                .set(getDslContext()
                        .newRecord(WALLET_IDENTITY_REFERENCE, reference))
                .onConflict(WALLET_IDENTITY_REFERENCE.WALLET_ID)
                .doNothing();
        execute(insertReturningStep);

        log.info("walletIdentityReference has been created, walletIdentityReference={} ", reference.toString());
    }

    @Override
    public WalletIdentityReference get(String id) {
        WalletIdentityReference walletIdentityReference = fetchOne(getDslContext()
                        .select(WALLET_IDENTITY_REFERENCE.WALLET_ID, WALLET_IDENTITY_REFERENCE.IDENTITY_ID)
                        .from(WALLET_IDENTITY_REFERENCE)
                        .where(WALLET_IDENTITY_REFERENCE.WALLET_ID.eq(id)),
                listRecordRowMapper);

        if (walletIdentityReference != null) {
            log.info("walletIdentityReference has been got, walletIdentityReference={}",
                    walletIdentityReference.toString());
        }

        return walletIdentityReference;
    }
}
