package dev.vality.wallets.hooker.kafka.serde;

import dev.vality.fistful.wallet.TimestampedChange;
import dev.vality.sink.common.parser.impl.MachineEventParser;
import dev.vality.sink.common.serialization.BinaryDeserializer;
import org.springframework.stereotype.Service;

@Service
public class WalletChangeMachineEventParser extends MachineEventParser<TimestampedChange> {

    public WalletChangeMachineEventParser(BinaryDeserializer<TimestampedChange> deserializer) {
        super(deserializer);
    }
}