package dev.vality.wallets.hooker.config;

import dev.vality.fistful.withdrawal.ManagementSrv;
import dev.vality.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;

@Configuration
public class FistfulClientConfig {

    @Value("${service.withdrawal.url}")
    private String withdrawalUrl;

    @Value("${service.withdrawal.networkTimeout}")
    private int networkTimeout;

    @Bean
    public ManagementSrv.Iface withdrawalFistfulClient() throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(URI.create(withdrawalUrl))
                .build(ManagementSrv.Iface.class);
    }
}