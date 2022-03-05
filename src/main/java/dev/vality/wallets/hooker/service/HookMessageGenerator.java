package dev.vality.wallets.hooker.service;

import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.webhook.dispatcher.WebhookMessage;

public interface HookMessageGenerator<T> {

    WebhookMessage generate(T event, WebHookModel model, MessageGenParams messageGenParams);

}
