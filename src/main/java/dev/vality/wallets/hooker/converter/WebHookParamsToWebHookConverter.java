package dev.vality.wallets.hooker.converter;

import dev.vality.fistful.webhooker.WebhookParams;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.utils.EventTypeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebHookParamsToWebHookConverter implements Converter<WebhookParams, WebHookModel> {

    @Override
    public WebHookModel convert(WebhookParams event) {
        WebHookModel webHookModel = WebHookModel.builder()
                .identityId(event.getIdentityId())
                .url(event.getUrl())
                .walletId(event.getWalletId())
                .eventTypes(EventTypeUtils.convertEventTypes(event))
                .build();

        log.info("webhookParams has been converted, webHookModel={}", webHookModel.toString());

        return webHookModel;
    }


}