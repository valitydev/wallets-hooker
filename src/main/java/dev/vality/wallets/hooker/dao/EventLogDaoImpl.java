package dev.vality.wallets.hooker.dao;

import dev.vality.mapper.RecordRowMapper;
import dev.vality.wallets.hooker.domain.enums.EventTopic;
import dev.vality.wallets.hooker.domain.tables.pojos.EventLog;
import dev.vality.wallets.hooker.domain.tables.records.EventLogRecord;
import org.jooq.InsertReturningStep;
import org.jooq.Query;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Optional;

import static dev.vality.wallets.hooker.domain.Tables.EVENT_LOG;

@Component
public class EventLogDaoImpl extends AbstractDao implements EventLogDao {

    private final RowMapper<EventLog> eventLogRowMapper;

    public EventLogDaoImpl(DataSource dataSource) {
        super(dataSource);
        this.eventLogRowMapper = new RecordRowMapper<>(EVENT_LOG, EventLog.class);
    }

    @Override
    public Optional<EventLog> get(String sourceId, Long eventId, EventTopic eventTopic) {
        Query query = getDslContext().selectFrom(EVENT_LOG)
                .where(EVENT_LOG.SOURCE_ID.eq(sourceId)
                        .and(EVENT_LOG.EVENT_ID.eq(eventId))
                        .and(EVENT_LOG.EVENT_TOPIC.eq(eventTopic)));

        return Optional.ofNullable(fetchOne(query, eventLogRowMapper));
    }

    @Override
    public void create(String sourceId, Long eventId, EventTopic eventTopic) {
        EventLog eventLog = new EventLog();
        eventLog.setSourceId(sourceId);
        eventLog.setEventId(eventId);
        eventLog.setEventTopic(eventTopic);
        InsertReturningStep<EventLogRecord> insertReturningStep = getDslContext()
                .insertInto(EVENT_LOG)
                .set(getDslContext()
                        .newRecord(EVENT_LOG, eventLog))
                .onConflict(EVENT_LOG.SOURCE_ID, EVENT_LOG.EVENT_ID, EVENT_LOG.EVENT_TOPIC)
                .doNothing();
        execute(insertReturningStep);
    }
}
