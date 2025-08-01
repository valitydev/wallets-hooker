package dev.vality.wallets.hooker.dao.webhook;

import dev.vality.mapper.RecordRowMapper;
import dev.vality.wallets.hooker.dao.AbstractDao;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.domain.tables.pojos.WebhookToEvents;
import dev.vality.wallets.hooker.domain.tables.records.WebhookToEventsRecord;
import org.jooq.InsertReturningStep;
import org.jooq.Record2;
import org.jooq.SelectConditionStep;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

import static dev.vality.wallets.hooker.domain.tables.WebhookToEvents.WEBHOOK_TO_EVENTS;

@Component
public class WebHookToEventsDaoImpl extends AbstractDao implements WebHookToEventsDao {

    private final RowMapper<WebhookToEvents> listRecordRowMapper;

    public WebHookToEventsDaoImpl(DataSource dataSource) {
        super(dataSource);
        this.listRecordRowMapper = new RecordRowMapper<>(WEBHOOK_TO_EVENTS, WebhookToEvents.class);
    }

    @Override
    public void create(WebhookToEvents webhookToEvents) {
        InsertReturningStep<WebhookToEventsRecord> insertReturningStep = getDslContext()
                .insertInto(WEBHOOK_TO_EVENTS)
                .set(getDslContext()
                        .newRecord(WEBHOOK_TO_EVENTS, webhookToEvents))
                .onConflict(WEBHOOK_TO_EVENTS.HOOK_ID, WEBHOOK_TO_EVENTS.EVENT_TYPE)
                .doNothing();
        execute(insertReturningStep);
    }

    @Override
    public List<WebhookToEvents> get(long id) {
        SelectConditionStep<Record2<Long, EventType>> where = getDslContext()
                .select(WEBHOOK_TO_EVENTS.HOOK_ID,
                        WEBHOOK_TO_EVENTS.EVENT_TYPE)
                .from(WEBHOOK_TO_EVENTS)
                .where(WEBHOOK_TO_EVENTS.HOOK_ID.eq(id));
        return fetch(where, listRecordRowMapper);
    }
}
