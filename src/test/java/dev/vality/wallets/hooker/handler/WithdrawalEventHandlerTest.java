package dev.vality.wallets.hooker.handler;

import dev.vality.fistful.withdrawal.ManagementSrv;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.config.PostgresqlSpringBootITest;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.wallets.hooker.service.WithdrawalClient;
import dev.vality.wallets.hooker.service.kafka.WithdrawalEventService;
import dev.vality.webhook.dispatcher.WebhookMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@PostgresqlSpringBootITest
class WithdrawalEventHandlerTest {

    @Autowired
    private WithdrawalEventService withdrawalEventService;

    @Autowired
    private WebHookDao webHookDao;

    @MockitoBean
    private WebHookMessageSenderService webHookMessageSenderService;

    @MockitoBean
    private WithdrawalClient withdrawalClient;

    @MockitoBean
    private ManagementSrv.Iface withdrawalFistfulClient;

    @Test
    void handleWithdrawalCreatedAndAndStatusChange() throws InterruptedException {
        WebHookModel webhook = TestBeanFactory.createWebhookModel();
        when(withdrawalClient.getWithdrawalInfo(eq(TestBeanFactory.WITHDRAWAL_ID), anyLong()))
                .thenReturn(TestBeanFactory.createWithdrawalState());

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

    @Test
    void handleWithdrawalSucceededWithNewBodyInSucceededWebhook() {
        WebHookModel webhook = TestBeanFactory.createWebhookModel();
        when(withdrawalClient.getWithdrawalInfo(TestBeanFactory.WITHDRAWAL_ID, 68L))
                .thenReturn(TestBeanFactory.createWithdrawalStateWithNewBody());

        webHookDao.create(webhook);

        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalEvent()));
        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalSucceeded(68L)));

        ArgumentCaptor<WebhookMessage> captor = ArgumentCaptor.forClass(WebhookMessage.class);
        verify(webHookMessageSenderService, timeout(1000L).times(2))
                .send(captor.capture());
        Assertions.assertTrue(captor.getAllValues().stream()
                .map(WebhookMessage::getRequestBody)
                .map(String::new)
                .anyMatch(body -> body.contains("\"eventType\":\"WithdrawalSucceeded\"")
                        && body.contains("\"body\":{\"amount\":1000,\"changedAmount\":1500,\"currency\":\"USD\"}")
                        && body.contains("\"fee\":{\"amount\":25,\"currency\":\"RUB\"}")));
    }

    @Test
    void handleWithdrawalAdjustmentSucceededWebhook() {
        WebHookModel webhook = TestBeanFactory.createWebhookModel();
        when(withdrawalClient.getWithdrawalInfo(eq(TestBeanFactory.WITHDRAWAL_ID), anyLong()))
                .thenReturn(TestBeanFactory.createWithdrawalStateWithNewBody());

        webHookDao.create(webhook);

        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalEvent()));
        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalSucceeded(69L)));
        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalAdjustmentChange(70L)));

        ArgumentCaptor<WebhookMessage> captor = ArgumentCaptor.forClass(WebhookMessage.class);
        verify(webHookMessageSenderService, timeout(1000L).times(3))
                .send(captor.capture());
        Assertions.assertTrue(captor.getAllValues().stream()
                .map(WebhookMessage::getRequestBody)
                .map(String::new)
                .anyMatch(body -> body.contains("\"eventType\":\"WithdrawalSucceeded\"")
                        && body.contains("\"body\":{\"amount\":1000,\"changedAmount\":1500,\"currency\":\"USD\"}")));
    }

    @Test
    void handleWithdrawalAdjustmentWithAdjustmentStateSucceededWebhook() {
        WebHookModel webhook = TestBeanFactory.createWebhookModel();
        when(withdrawalClient.getWithdrawalInfo(eq(TestBeanFactory.WITHDRAWAL_ID), anyLong()))
                .thenReturn(TestBeanFactory.createWithdrawalStateWithAdjustmentState());

        webHookDao.create(webhook);

        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalEvent()));
        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalSucceeded(69L)));
        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalAdjustmentChange(70L)));

        ArgumentCaptor<WebhookMessage> captor = ArgumentCaptor.forClass(WebhookMessage.class);
        verify(webHookMessageSenderService, timeout(1000L).times(3))
                .send(captor.capture());
        Assertions.assertTrue(captor.getAllValues().stream()
                .map(WebhookMessage::getRequestBody)
                .map(String::new)
                .anyMatch(body -> body.contains("\"eventType\":\"WithdrawalSucceeded\"")
                        && body.contains("\"body\":{\"amount\":1000,\"changedAmount\":1500,\"currency\":\"USD\"}")));
    }
}
