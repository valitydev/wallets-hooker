package dev.vality.wallets.hooker.converter;

import dev.vality.fistful.webhooker.Webhook;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.utils.WebHookConverterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebHookModelToWebHookConverter implements Converter<WebHookModel, Webhook> {

    @Override
    public Webhook convert(WebHookModel event) {
        Webhook webhook = new Webhook();
        webhook.setId(event.getId());
        webhook.setEnabled(event.getEnabled());
        webhook.setPartyId(event.getPartyId());
        webhook.setWalletId(event.getWalletId());
        webhook.setPubKey(event.getPubKey());
        webhook.setEventFilter(WebHookConverterUtils.generateEventFilter(event.getEventTypes()));
        webhook.setUrl(event.getUrl());

        log.info("webHookModel has been converted, webhookDamsel={}", webhook);

        return webhook;
    }

}