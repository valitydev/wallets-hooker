package dev.vality.wallets.hooker.handler;

import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.wallets.hooker.service.kafka.DestinationEventService;
import dev.vality.wallets.hooker.service.kafka.WalletEventService;
import dev.vality.wallets.hooker.service.kafka.WithdrawalEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = "fistful.pollingEnabled=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WaitingWithdrawalReferenceEventHandlerTest extends EmbeddedPostgresIntegrationTest {

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
