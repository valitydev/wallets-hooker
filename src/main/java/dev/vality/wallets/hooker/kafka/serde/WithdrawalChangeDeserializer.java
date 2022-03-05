package dev.vality.wallets.hooker.kafka.serde;

import dev.vality.fistful.withdrawal.TimestampedChange;
import dev.vality.sink.common.serialization.impl.AbstractThriftBinaryDeserializer;
import org.springframework.stereotype.Service;

@Service
public class WithdrawalChangeDeserializer extends AbstractThriftBinaryDeserializer<TimestampedChange> {

    @Override
    public TimestampedChange deserialize(byte[] bin) {
        return deserialize(bin, new TimestampedChange());
    }
}
