package dev.vality.wallets.hooker.kafka;

import dev.vality.fistful.webhooker.*;
import dev.vality.kafka.common.serialization.ThriftSerializer;
import dev.vality.wallets.hooker.config.KafkaPostgresqlSpringBootITest;
import dev.vality.wallets.hooker.handler.TestBeanFactory;
import dev.vality.wallets.hooker.service.WebHookMessageSenderService;
import dev.vality.wallets.hooker.service.kafka.DestinationEventService;
import dev.vality.wallets.hooker.service.kafka.WithdrawalEventService;
import dev.vality.webhook.dispatcher.WebhookMessage;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KafkaPostgresqlSpringBootITest
@TestPropertySource(properties = "merchant.callback.timeout=1")
class WebhookServiceTest {

    private static final String TEST = "/test";
    private static final String URL_2 = TEST + "/qwe";

    @Value("${kafka.topic.hook.name}")
    private String topicName;

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Autowired
    private WebhookManagerSrv.Iface requestHandler;

    @Autowired
    private DestinationEventService destinationEventService;

    @Autowired
    private WithdrawalEventService withdrawalEventService;

    @Test
    void testFlow() throws TException {
        DestinationEventType created = DestinationEventType.created(new DestinationCreated());
        WebhookParams webhookParams = new WebhookParams()
                .setEventFilter(new EventFilter().setTypes(Set.of(EventType.destination(created))))
                .setPartyId(TestBeanFactory.PARTY_ID)
                .setUrl(TEST);
        Webhook webhook = requestHandler.create(webhookParams);

        ThriftSerializer<Webhook> webhookThriftSerializer = new ThriftSerializer<>();

        byte[] serialize = webhookThriftSerializer.serialize("t", webhook);
        assertTrue(serialize.length > 0);
        assertEquals(TEST, webhook.getUrl());

        webhookParams.setUrl(URL_2);
        requestHandler.create(webhookParams);
        List<Webhook> list = requestHandler.getList(webhookParams.getPartyId());
        assertEquals(2L, list.size());

        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestination()));
        destinationEventService.handleEvents(List.of(TestBeanFactory.createDestinationAccount()));

        webhookParams = new WebhookParams()
                .setEventFilter(new EventFilter()
                        .setTypes(Set.of(EventType.withdrawal(WithdrawalEventType.started(new WithdrawalStarted())),
                                EventType.withdrawal(WithdrawalEventType.succeeded(new WithdrawalSucceeded())))))
                .setPartyId(TestBeanFactory.PARTY_ID)
                .setWalletId(TestBeanFactory.SOURCE_WALLET_ID)
                .setUrl(TEST);
        requestHandler.create(webhookParams);

        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalEvent()));
        withdrawalEventService.handleEvents(List.of(TestBeanFactory.createWithdrawalSucceeded()));

        Consumer<String, WebhookMessage> consumer = createConsumer(WebHookDeserializer.class);

        consumer.subscribe(List.of(topicName));
        ConsumerRecords<String, WebhookMessage> poll = consumer.poll(Duration.ofMillis(5000));
        Iterable<ConsumerRecord<String, WebhookMessage>> records = poll.records(topicName);

        assertEquals(4L, poll.count());

        ArrayList<WebhookMessage> webhookMessages = new ArrayList<>();
        records.forEach(consumerRecord -> webhookMessages.add(consumerRecord.value()));

        assertEquals(TestBeanFactory.DESTINATION, webhookMessages.get(0).source_id);
        assertEquals(TestBeanFactory.DESTINATION, webhookMessages.get(1).source_id);
        assertEquals(TestBeanFactory.WITHDRAWAL_ID, webhookMessages.get(2).source_id);
        assertEquals(TestBeanFactory.WITHDRAWAL_ID, webhookMessages.get(3).source_id);
    }

    private <T> Consumer<String, T> createConsumer(Class clazz) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, clazz);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }

}
