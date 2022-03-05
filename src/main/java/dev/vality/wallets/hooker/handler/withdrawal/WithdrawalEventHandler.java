package dev.vality.wallets.hooker.handler.withdrawal;

import dev.vality.fistful.withdrawal.TimestampedChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.handler.EventHandler;

public interface WithdrawalEventHandler extends EventHandler<TimestampedChange, MachineEvent> {
}
