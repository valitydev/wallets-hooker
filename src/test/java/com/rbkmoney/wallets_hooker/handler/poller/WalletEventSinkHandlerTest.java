package com.rbkmoney.wallets_hooker.handler.poller;

import com.rbkmoney.eventstock.client.EventAction;
import com.rbkmoney.fistful.withdrawal.SinkEvent;
import com.rbkmoney.wallets_hooker.HookerApplication;
import com.rbkmoney.wallets_hooker.constant.EventTopic;
import com.rbkmoney.wallets_hooker.dao.AbstractPostgresIntegrationTest;
import com.rbkmoney.wallets_hooker.dao.EventLogDao;
import com.rbkmoney.wallets_hooker.dao.webhook.WebHookDao;
import com.rbkmoney.wallets_hooker.domain.WebHookModel;
import com.rbkmoney.wallets_hooker.service.WebHookMessageSenderService;
import com.rbkmoney.wallets_hooker.service.kafka.DestinationEventService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = HookerApplication.class)
@TestPropertySource(properties = "fistful.pollingEnabled=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WalletEventSinkHandlerTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private WalletEventSinkHandler walletEventSinkHandler;

    @Autowired
    private WithdrawalEventSinkHandler withdrawalEventSinkHandler;

    @Autowired
    private DestinationEventService destinationEventService;

    @Autowired
    private WebHookDao webHookDao;

    @Autowired
    private EventLogDao eventLogDao;

    @MockBean
    private WebHookMessageSenderService webHookMessageSenderService;

    @Test
    public void handle() {
        WebHookModel webhook = TestBeanFactory.createWebhookModel();

        webHookDao.create(webhook);

        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestination()));
        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestinationAccount()));

        EventAction action = walletEventSinkHandler.handle(TestBeanFactory.createWalletEvent(), "test");
        assertEquals(action, EventAction.CONTINUE);

        action = withdrawalEventSinkHandler.handle(TestBeanFactory.createWithdrawalEvent(), "test");
        assertEquals(action, EventAction.CONTINUE);

        verify(webHookMessageSenderService, times(1))
                .send(any());

        SinkEvent sinkEvent = TestBeanFactory.createWithdrawalSucceeded();

        action = withdrawalEventSinkHandler.handle(sinkEvent, "test");
        assertEquals(action, EventAction.CONTINUE);
        verify(webHookMessageSenderService, times(2))
                .send(any());

        Long lastEventId = eventLogDao.getLastEventId(EventTopic.DESTINATION, 0L);
        assertEquals(2L, lastEventId.longValue());

        lastEventId = eventLogDao.getLastEventId(EventTopic.WALLET, 0L);
        assertEquals(TestBeanFactory.WALLET_ID, lastEventId.longValue());

        lastEventId = eventLogDao.getLastEventId(EventTopic.WITHDRAWAL, 0L);
        assertEquals(67L, lastEventId.longValue());
    }
}