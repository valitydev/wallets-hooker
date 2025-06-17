package dev.vality.wallets.hooker.handler;

import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.config.PostgresqlSpringBootITest;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.wallets.hooker.service.kafka.WithdrawalEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@PostgresqlSpringBootITest
class WithdrawalEventHandlerTest {

    @Autowired
    private WithdrawalEventService withdrawalEventService;

    @Autowired
    private WebHookDao webHookDao;

    @MockitoBean
    private WebHookMessageSenderService webHookMessageSenderService;

    @Test
    void handleWithdrawalCreatedAndAndStatusChange() throws InterruptedException {
        WebHookModel webhook = TestBeanFactory.createWebhookModel();

        webHookDao.create(webhook);

        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            MachineEvent event = TestBeanFactory.createWithdrawalSucceeded();
            withdrawalEventService.handleEvents(List.of(event));
            latch.countDown();
        }).start();

        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalEvent()));
        verify(webHookMessageSenderService, timeout(1000L).times(2))
                .send(any());

        latch.await();
    }
}
