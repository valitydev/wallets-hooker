package dev.vality.wallets.hooker.dao.withdrawal;

import dev.vality.mapper.RecordRowMapper;
import dev.vality.wallets.hooker.dao.AbstractDao;
import dev.vality.wallets.hooker.domain.tables.pojos.WithdrawalReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import static dev.vality.wallets.hooker.domain.tables.WithdrawalReference.WITHDRAWAL_REFERENCE;

@Component
@Slf4j
public class WithdrawalReferenceDaoImpl extends AbstractDao implements WithdrawalReferenceDao {

    private final RowMapper<WithdrawalReference> listRecordRowMapper;

    public WithdrawalReferenceDaoImpl(DataSource dataSource) {
        super(dataSource);
        this.listRecordRowMapper =
                new RecordRowMapper<>(WITHDRAWAL_REFERENCE, WithdrawalReference.class);
    }

    @Override
    public void create(WithdrawalReference reference) {
        var insertReturningStep = getDslContext()
                .insertInto(WITHDRAWAL_REFERENCE)
                .set(getDslContext()
                        .newRecord(WITHDRAWAL_REFERENCE, reference))
                .onConflict(WITHDRAWAL_REFERENCE.WITHDRAWAL_ID)
                .doNothing();
        execute(insertReturningStep);
        log.info("WithdrawalReference has been created, withdrawalReference={} ",
                reference.toString());
    }

    @Override
    public WithdrawalReference get(String id) {
        var withdrawalReference = fetchOne(getDslContext()
                        .select(WITHDRAWAL_REFERENCE.WITHDRAWAL_ID,
                                WITHDRAWAL_REFERENCE.WALLET_ID,
                                WITHDRAWAL_REFERENCE.PARTY_ID,
                                WITHDRAWAL_REFERENCE.EVENT_ID,
                                WITHDRAWAL_REFERENCE.EXTERNAL_ID)
                        .from(WITHDRAWAL_REFERENCE)
                        .where(WITHDRAWAL_REFERENCE.WITHDRAWAL_ID.eq(id)),
                listRecordRowMapper);

        if (withdrawalReference != null) {
            log.info("withdrawalReference has been got, withdrawalReference={}",
                    withdrawalReference);
        }

        return withdrawalReference;
    }
}
