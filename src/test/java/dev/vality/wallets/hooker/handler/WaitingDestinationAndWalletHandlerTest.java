package dev.vality.wallets.hooker.handler;

import dev.vality.fistful.destination.*;
import dev.vality.kafka.common.serialization.ThriftSerializer;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.machinegun.msgpack.Value;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.wallets.hooker.service.kafka.DestinationEventService;
import dev.vality.wallets.hooker.service.kafka.WalletEventService;
import dev.vality.wallets.hooker.service.kafka.WithdrawalEventService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@TestPropertySource(properties = "fistful.pollingEnabled=false")
public class WaitingDestinationAndWalletHandlerTest extends EmbeddedPostgresIntegrationTest {

    @Autowired
    private WalletEventService walletEventService;

    @Autowired
    private WithdrawalEventService withdrawalEventService;

    @Autowired
    private DestinationEventService destinationEventService;

    @Autowired
    private WebHookDao webHookDao;

    @MockBean
    private WebHookMessageSenderService webHookMessageSenderService;

    @Test
    public void handleWaitingDestinationAndWallet() throws InterruptedException {
        WebHookModel webhook = TestBeanFactory.createWebhookModel();

        webHookDao.create(webhook);
        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestination()));
        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestinationAccount()));

        MachineEvent destination = TestBeanFactory.createDestination();
        dev.vality.fistful.destination.Change change = new dev.vality.fistful.destination.Change();
        change.setStatus(StatusChange.changed(Status.authorized(new Authorized())));
        destination.setData(Value.bin(new ThriftSerializer<>().serialize("", new TimestampedChange()
                .setChange(change)
                .setOccuredAt("2016-03-22T06:12:27Z"))));
        destinationEventService.handleEvents(List.of(destination));

        change.setStatus(StatusChange.changed(Status.unauthorized(new Unauthorized())));
        destination.setData(Value.bin(new ThriftSerializer<>().serialize("", new TimestampedChange()
                .setChange(change)
                .setOccuredAt("2016-03-22T06:12:27Z"))));
        destinationEventService.handleEvents(List.of(destination));
        walletEventService.handleEvents(List.of(TestBeanFactory.createWalletEvent()));

        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            MachineEvent event = TestBeanFactory.createWithdrawalSucceeded();
            withdrawalEventService.handleEvents(List.of(event));
            latch.countDown();
        }).start();

        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalEvent()));
        verify(webHookMessageSenderService, times(1))
                .send(any());

        latch.await();
    }

}