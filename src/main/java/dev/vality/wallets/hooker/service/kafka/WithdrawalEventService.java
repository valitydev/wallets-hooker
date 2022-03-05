package dev.vality.wallets.hooker.service.kafka;

import dev.vality.fistful.withdrawal.TimestampedChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.sink.common.parser.impl.MachineEventParser;
import dev.vality.wallets.hooker.dao.EventLogDao;
import dev.vality.wallets.hooker.handler.withdrawal.WithdrawalEventHandler;
import dev.vality.wallets.hooker.domain.enums.EventTopic;
import dev.vality.wallets.hooker.domain.tables.pojos.EventLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalEventService {

    private final List<WithdrawalEventHandler> withdrawalEventHandlers;
    private final MachineEventParser<TimestampedChange> parser;
    private final EventLogDao eventLogDao;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void handleEvents(List<MachineEvent> machineEvents) {
        machineEvents.forEach(this::handleIfAccept);
    }

    private void handleIfAccept(MachineEvent machineEvent) {
        Optional<EventLog> duplicate = eventLogDao.get(
                machineEvent.getSourceId(),
                machineEvent.getEventId(),
                EventTopic.withdrawal);

        if (duplicate.isPresent()) {
            return;
        }

        TimestampedChange change = parser.parse(machineEvent);

        if (change.isSetChange()) {
            withdrawalEventHandlers.stream()
                    .filter(handler -> handler.accept(change))
                    .forEach(handler -> handler.handle(change, machineEvent));
        }

        eventLogDao.create(
                machineEvent.getSourceId(),
                machineEvent.getEventId(),
                EventTopic.withdrawal);
    }
}
