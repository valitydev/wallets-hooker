package dev.vality.wallets.hooker.dao;

import dev.vality.wallets.hooker.domain.enums.EventTopic;
import dev.vality.wallets.hooker.domain.tables.pojos.EventLog;

import java.util.Optional;

public interface EventLogDao {

    Optional<EventLog> get(String sourceId, Long eventId, EventTopic eventTopic);

    void create(String sourceId, Long eventId, EventTopic eventTopic);

}
