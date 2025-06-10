package dev.vality.wallets.hooker.config;

import dev.vality.testcontainers.annotations.KafkaConfig;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainerSingleton;
import dev.vality.testcontainers.annotations.postgresql.PostgresqlTestcontainerSingleton;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PostgresqlTestcontainerSingleton
@KafkaTestcontainerSingleton(
        properties = {
                "kafka.topic.wallet.listener.enabled=true",
                "kafka.topic.withdrawal.listener.enabled=true",
                "kafka.topic.destination.listener.enabled=true"},
        topicsKeys = {
                "kafka.topic.hook.name",
                "kafka.topic.wallet.name",
                "kafka.topic.withdrawal.name",
                "kafka.topic.destination.name"})
@SpringBootTest
@KafkaConfig
public @interface KafkaPostgresqlSpringBootITest {
}
