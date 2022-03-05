package dev.vality.wallets.hooker.handler.destination;

import dev.vality.fistful.destination.TimestampedChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.handler.EventHandler;

public interface DestinationEventHandler extends EventHandler<TimestampedChange, MachineEvent> {
}
