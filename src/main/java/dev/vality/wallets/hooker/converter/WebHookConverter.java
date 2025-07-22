package dev.vality.wallets.hooker.converter;

import dev.vality.wallets.hooker.dao.party.PartyKeyDao;
import dev.vality.wallets.hooker.dao.webhook.WebHookToEventsDao;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.domain.tables.pojos.Webhook;
import dev.vality.wallets.hooker.domain.tables.pojos.WebhookToEvents;
import dev.vality.wallets.hooker.utils.WebHookConverterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebHookConverter implements Converter<Webhook, dev.vality.fistful.webhooker.Webhook> {

    private final PartyKeyDao partyKeyDao;
    private final WebHookToEventsDao webHookToEventsDao;

    @Override
    public dev.vality.fistful.webhooker.Webhook convert(Webhook webhook) {
        var partyKey = partyKeyDao.getByParty(webhook.getPartyId());
        Set<EventType> eventTypes = webHookToEventsDao.get(webhook.getId()).stream()
                .map(WebhookToEvents::getEventType)
                .collect(Collectors.toSet());

        var webhookDamsel = new dev.vality.fistful.webhooker.Webhook();
        webhookDamsel.setId(webhook.getId());
        webhookDamsel.setEnabled(webhook.getEnabled());
        webhookDamsel.setPartyId(webhook.getPartyId());
        webhookDamsel.setWalletId(webhook.getWalletId());
        webhookDamsel.setPubKey(partyKey.getPubKey());
        webhookDamsel.setUrl(webhook.getUrl());
        webhookDamsel.setEventFilter(WebHookConverterUtils.generateEventFilter(eventTypes));

        log.info("webhook has been converted, webhookDamsel={}", webhookDamsel);

        return webhookDamsel;
    }
}
