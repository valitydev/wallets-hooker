package dev.vality.wallets.hooker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Slf4j
@Configuration
public class RetryConfig {

    @Value("${service.withdrawal.retry.maxAttempts}")
    private int maxAttempts;

    @Value("${service.withdrawal.retry.initialIntervalMs}")
    private long initialIntervalMs;

    @Value("${service.withdrawal.retry.multiplier}")
    private double multiplier;

    @Value("${service.withdrawal.retry.maxIntervalMs}")
    private long maxIntervalMs;

    @Bean
    public RetryTemplate withdrawalRetryTemplate() {
        var retryTemplate = new RetryTemplate();

        var retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);

        var backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialIntervalMs);
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxIntervalMs);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}
