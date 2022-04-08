package dev.vality.wallets.hooker.handler;

import dev.vality.testcontainers.annotations.DefaultSpringBootTest;
import dev.vality.testcontainers.annotations.postgresql.PostgresqlTestcontainer;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.wallets.hooker.service.kafka.DestinationEventService;
import dev.vality.wallets.hooker.service.kafka.WalletEventService;
import dev.vality.wallets.hooker.service.kafka.WithdrawalEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@PostgresqlTestcontainer
@DefaultSpringBootTest
public class WaitingWithdrawalReferenceEventHandlerTest {

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
    public void handleWaitingWithdrawalReference() throws InterruptedException {
        WebHookModel webhook = TestBeanFactory.createWebhookModel();
        webHookDao.create(webhook);

        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestination()));

        CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> {
            withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalEvent()));
            verify(webHookMessageSenderService, times(1))
                    .send(any());
            latch.countDown();
        }).start();

        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestinationAccount()));
        walletEventService.handleEvents(List.of(TestBeanFactory.createWalletEvent()));

        latch.await();
    }
}
