package dev.vality.wallets.hooker.config;


import dev.vality.kafka.common.serialization.ThriftSerializer;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.wallets.hooker.kafka.serde.SinkEventDeserializer;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.SeekToCurrentBatchErrorHandler;

import java.util.Map;

import static org.apache.kafka.clients.consumer.OffsetResetStrategy.EARLIEST;

@Slf4j
@Configuration
public class KafkaConfig {

    @Value("${kafka.consumer.concurrency}")
    private int consumerConcurrency;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MachineEvent> destinationEventListenerContainerFactory(
            KafkaProperties kafkaProperties
    ) {
        return listenerContainerFactory(kafkaProperties);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MachineEvent> withdrawalEventListenerContainerFactory(
            KafkaProperties kafkaProperties
    ) {
        return listenerContainerFactory(kafkaProperties);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MachineEvent> walletEventListenerContainerFactory(
            KafkaProperties kafkaProperties
    ) {
        return listenerContainerFactory(kafkaProperties);
    }

    @Bean
    public KafkaTemplate<String, WebhookMessage> kafkaTemplate(KafkaProperties kafkaProperties) {
        return new KafkaTemplate<>(producerFactory(kafkaProperties));
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, MachineEvent> listenerContainerFactory(
            KafkaProperties kafkaProperties
    ) {
        ConcurrentKafkaListenerContainerFactory<String, MachineEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        DefaultKafkaConsumerFactory<String, MachineEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerConfig(kafkaProperties));

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(consumerConcurrency);
        factory.setBatchErrorHandler(new SeekToCurrentBatchErrorHandler());
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }

    private Map<String, Object> consumerConfig(KafkaProperties kafkaProperties) {
        var config = kafkaProperties.buildConsumerProperties();
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SinkEventDeserializer.class);
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, EARLIEST.name().toLowerCase());
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return config;
    }

    private ProducerFactory<String, WebhookMessage> producerFactory(KafkaProperties kafkaProperties) {
        var config = kafkaProperties.buildProducerProperties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ThriftSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }
}
