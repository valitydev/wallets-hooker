package dev.vality.wallets.hooker.service;

import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseHookMessageGenerator<T> implements HookMessageGenerator<T> {

    protected final Long parentIsNotExistId;

    @Override
    public final WebhookMessage generate(T event, WebHookModel model, MessageGenParams messageGenParams) {
        if (messageGenParams.getParentId() == null) {
            messageGenParams.setParentId(parentIsNotExistId);
        }

        return generateMessage(event, model, messageGenParams);
    }

    protected abstract WebhookMessage generateMessage(T event, WebHookModel model, MessageGenParams messageGenParams);

}
