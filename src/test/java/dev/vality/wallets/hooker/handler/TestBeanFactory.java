package dev.vality.wallets.hooker.handler;

import dev.vality.fistful.account.Account;
import dev.vality.fistful.base.*;
import dev.vality.fistful.cashflow.CashFlowAccount;
import dev.vality.fistful.cashflow.FinalCashFlow;
import dev.vality.fistful.cashflow.FinalCashFlowAccount;
import dev.vality.fistful.cashflow.FinalCashFlowPosting;
import dev.vality.fistful.cashflow.SystemCashFlowAccount;
import dev.vality.fistful.cashflow.WalletCashFlowAccount;
import dev.vality.fistful.destination.Destination;
import dev.vality.fistful.destination.TimestampedChange;
import dev.vality.fistful.withdrawal.CreatedChange;
import dev.vality.fistful.withdrawal.StatusChange;
import dev.vality.fistful.withdrawal.Withdrawal;
import dev.vality.fistful.withdrawal.WithdrawalState;
import dev.vality.fistful.withdrawal.status.Status;
import dev.vality.fistful.withdrawal.status.Succeeded;
import dev.vality.kafka.common.serialization.ThriftSerializer;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.machinegun.msgpack.Value;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import org.apache.thrift.TBase;

import java.util.LinkedHashSet;
import java.util.List;

public class TestBeanFactory {

    public static final String SOURCE_WALLET_ID = "sourceWalletId";
    public static final String PARTY_ID = "partyId";
    public static final String DESTINATION = "destination";
    public static final String WITHDRAWAL_ID = "withdrawalId";

    public static MachineEvent createDestination() {
        Destination destination = new Destination();
        destination.setId("destinationId");
        destination.setName("name");
        destination.setExternalId("externalId");
        destination.setRealm(Realm.test);
        destination.setPartyId(PARTY_ID);
        destination.setCreatedAt("2025-03-22T06:12:27Z");
        BankCard bankCard = new BankCard();
        bankCard.setBin("1234");
        bankCard.setMaskedPan("421");
        bankCard.setPaymentSystem(new PaymentSystemRef("mastercard"));
        bankCard.setToken("token");
        Resource resource = new Resource();
        resource.setBankCard(new ResourceBankCard(bankCard));
        destination.setResource(resource);
        dev.vality.fistful.destination.Change change = new dev.vality.fistful.destination.Change();
        change.setCreated(destination);

        TimestampedChange timestampedChange = new TimestampedChange()
                .setOccuredAt("2016-03-22T06:12:27Z")
                .setChange(change);

        return machineEvent(
                DESTINATION,
                1L,
                new ThriftSerializer<>(),
                timestampedChange);
    }

    public static MachineEvent createDestinationAccount() {
        Account account = new Account();
        account.setAccountId(123L);
        account.setCurrency(new CurrencyRef().setSymbolicCode("RUB"));
        account.setPartyId(PARTY_ID);
        account.setRealm(Realm.test);
        dev.vality.fistful.destination.AccountChange accountChange =
                new dev.vality.fistful.destination.AccountChange();
        accountChange.setCreated(account);
        dev.vality.fistful.destination.Change change = new dev.vality.fistful.destination.Change();
        change.setAccount(accountChange);

        TimestampedChange timestampedChange = new TimestampedChange()
                .setOccuredAt("2016-03-22T06:12:27Z")
                .setChange(change);

        return machineEvent(
                DESTINATION,
                2L,
                new ThriftSerializer<>(),
                timestampedChange);
    }

    public static MachineEvent createWithdrawalEvent() {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setDestinationId(DESTINATION);
        withdrawal.setExternalId("extId");
        withdrawal.setWalletId(SOURCE_WALLET_ID);
        withdrawal.setId(WITHDRAWAL_ID);
        withdrawal.setCreatedAt("2025-03-22T06:12:27Z");
        withdrawal.setPartyId(PARTY_ID);
        withdrawal.setId(WITHDRAWAL_ID);
        withdrawal.setDomainRevision(1L);

        Cash body = new Cash();
        body.setAmount(1000);
        CurrencyRef currency = new CurrencyRef();
        currency.setSymbolicCode("RUB");
        body.setCurrency(currency);
        withdrawal.setBody(body);
        dev.vality.fistful.withdrawal.Change change = new dev.vality.fistful.withdrawal.Change();
        change.setCreated(new CreatedChange()
                .setWithdrawal(withdrawal));

        dev.vality.fistful.withdrawal.TimestampedChange timestampedChange =
                new dev.vality.fistful.withdrawal.TimestampedChange()
                        .setOccuredAt("2016-03-22T06:12:27Z")
                        .setChange(change);

        return machineEvent(
                WITHDRAWAL_ID,
                66L,
                new ThriftSerializer<>(),
                timestampedChange);
    }

    public static MachineEvent createWithdrawalSucceeded() {
        return createWithdrawalSucceeded(67L);
    }

    public static MachineEvent createWithdrawalSucceeded(Long eventId) {
        dev.vality.fistful.withdrawal.Change change = new dev.vality.fistful.withdrawal.Change();
        change.setStatusChanged(new StatusChange().setStatus(Status.succeeded(new Succeeded())));

        dev.vality.fistful.withdrawal.TimestampedChange timestampedChange =
                new dev.vality.fistful.withdrawal.TimestampedChange()
                        .setOccuredAt("2016-03-22T06:12:27Z")
                        .setChange(change);

        return machineEvent(
                WITHDRAWAL_ID,
                eventId,
                new ThriftSerializer<>(),
                timestampedChange);
    }

    public static WithdrawalState createWithdrawalState() {
        return new WithdrawalState()
                .setId(WITHDRAWAL_ID)
                .setDestinationId(DESTINATION)
                .setExternalId("extId")
                .setWalletId(SOURCE_WALLET_ID)
                .setPartyId(PARTY_ID)
                .setCreatedAt("2025-03-22T06:12:27Z")
                .setDomainRevision(1L)
                .setBody(createCash(1000));
    }

    public static WithdrawalState createWithdrawalStateWithNewBody() {
        return createWithdrawalState()
                .setNewBody(createCash(1500, "USD"))
                .setEffectiveFinalCashFlow(new FinalCashFlow()
                        .setPostings(List.of(createFeePosting())));
    }

    public static WebHookModel createWebhookModel() {
        LinkedHashSet<EventType> eventTypes = new LinkedHashSet<>();
        eventTypes.add(EventType.WITHDRAWAL_CREATED);
        eventTypes.add(EventType.WITHDRAWAL_SUCCEEDED);
        return WebHookModel.builder()
                .enabled(true)
                .partyId(TestBeanFactory.PARTY_ID)
                .url("/qwe")
                .walletId(TestBeanFactory.SOURCE_WALLET_ID)
                .eventTypes(eventTypes)
                .build();
    }

    @SuppressWarnings("rawtypes")
    private static <T extends TBase> MachineEvent machineEvent(
            String sourceId,
            Long eventId,
            ThriftSerializer<T> depositChangeSerializer,
            T change) {
        return new MachineEvent()
                .setEventId(eventId)
                .setSourceId(sourceId)
                .setSourceNs("source_ns")
                .setCreatedAt("2016-03-22T06:12:27Z")
                .setData(Value.bin(depositChangeSerializer.serialize("", change)));
    }

    private static Cash createCash(long amount) {
        return createCash(amount, "RUB");
    }

    private static Cash createCash(long amount, String currencyCode) {
        Cash body = new Cash();
        body.setAmount(amount);
        CurrencyRef currency = new CurrencyRef();
        currency.setSymbolicCode(currencyCode);
        body.setCurrency(currency);
        return body;
    }

    private static FinalCashFlowPosting createFeePosting() {
        return new FinalCashFlowPosting()
                .setSource(new FinalCashFlowAccount()
                        .setAccountType(CashFlowAccount.wallet(WalletCashFlowAccount.sender_source)))
                .setDestination(new FinalCashFlowAccount()
                        .setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)))
                .setVolume(createCash(25));
    }
}
