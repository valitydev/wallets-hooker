package dev.vality.wallets.hooker.handler.wallet;

import dev.vality.fistful.wallet.TimestampedChange;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.handler.EventHandler;

public interface WalletEventHandler extends EventHandler<TimestampedChange, MachineEvent> {
}
