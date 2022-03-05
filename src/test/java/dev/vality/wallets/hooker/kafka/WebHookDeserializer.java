package dev.vality.wallets.hooker.kafka;


import dev.vality.kafka.common.serialization.AbstractThriftDeserializer;
import dev.vality.webhook.dispatcher.WebhookMessage;

public class WebHookDeserializer extends AbstractThriftDeserializer<WebhookMessage> {

    @Override
    public WebhookMessage deserialize(String s, byte[] bytes) {
        return super.deserialize(bytes, new WebhookMessage());
    }

}
