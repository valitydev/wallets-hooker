package dev.vality.wallets.hooker.handler;

import dev.vality.wallets.hooker.config.PostgresqlSpringBootITest;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.wallets.hooker.service.kafka.DestinationEventService;
import dev.vality.wallets.hooker.service.kafka.WalletEventService;
import dev.vality.wallets.hooker.service.kafka.WithdrawalEventService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@PostgresqlSpringBootITest
@TestPropertySource(properties = "fistful.pollingEnabled=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WalletEventSinkEventHandlerTest {

    @Autowired
    private WalletEventService walletEventService;

    @Autowired
    private WithdrawalEventService withdrawalEventService;

    @Autowired
    private DestinationEventService destinationEventService;

    @Autowired
    private WebHookDao webHookDao;

    @MockitoBean
    private WebHookMessageSenderService webHookMessageSenderService;

    @Test
    void handle() {
        WebHookModel webhook = TestBeanFactory.createWebhookModel();

        webHookDao.create(webhook);

        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestination()));
        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestinationAccount()));
        walletEventService.handleEvents(List.of(TestBeanFactory.createWalletEvent()));
        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalEvent()));

        verify(webHookMessageSenderService, times(1))
                .send(any());

        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalSucceeded()));
        verify(webHookMessageSenderService, times(2))
                .send(any());
    }
}