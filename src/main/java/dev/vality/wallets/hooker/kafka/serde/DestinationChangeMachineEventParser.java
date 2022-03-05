package dev.vality.wallets.hooker.kafka.serde;

import dev.vality.fistful.destination.TimestampedChange;
import dev.vality.sink.common.parser.impl.MachineEventParser;
import dev.vality.sink.common.serialization.BinaryDeserializer;
import org.springframework.stereotype.Service;

@Service
public class DestinationChangeMachineEventParser extends MachineEventParser<TimestampedChange> {

    public DestinationChangeMachineEventParser(BinaryDeserializer<TimestampedChange> deserializer) {
        super(deserializer);
    }
}