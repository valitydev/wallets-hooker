package dev.vality.wallets.hooker.handler;

import dev.vality.wallets.hooker.config.PostgresqlSpringBootITest;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.wallets.hooker.service.WithdrawalClient;
import dev.vality.fistful.withdrawal.ManagementSrv;
import dev.vality.wallets.hooker.service.kafka.DestinationEventService;
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
class DestinationEventHandlerTest {

    @Autowired
    private DestinationEventService destinationEventService;

    @Autowired
    private WebHookDao webHookDao;

    @MockitoBean
    private WebHookMessageSenderService webHookMessageSenderService;

    @MockitoBean
    private WithdrawalClient withdrawalClient;

    @MockitoBean
    private ManagementSrv.Iface withdrawalFistfulClient;

    @Test
    void failHandleDestinationCreated() {
        WebHookModel webhook = TestBeanFactory.createWebhookModel();

        webHookDao.create(webhook);

        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestination()));
        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestinationAccount()));

        verify(webHookMessageSenderService, times(0))
                .send(any());
    }

    @Test
    void handleDestinationCreatedAndAccountChange() {
        WebHookModel webhook = TestBeanFactory.createWebhookModel();
        webhook.getEventTypes().add(EventType.DESTINATION_CREATED);

        webHookDao.create(webhook);

        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestination()));
        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestinationAccount()));

        verify(webHookMessageSenderService, times(1))
                .send(any());
    }
}
