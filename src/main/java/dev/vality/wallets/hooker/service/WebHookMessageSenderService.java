package dev.vality.wallets.hooker.service;

import dev.vality.webhook.dispatcher.WebhookMessage;

public interface WebHookMessageSenderService {

    void send(WebhookMessage webhookMessage);

}
