package dev.vality.wallets.hooker.service;

import dev.vality.fistful.webhooker.Webhook;
import dev.vality.fistful.webhooker.WebhookManagerSrv;
import dev.vality.fistful.webhooker.WebhookNotFound;
import dev.vality.fistful.webhooker.WebhookParams;
import dev.vality.wallets.hooker.converter.WebHookConverter;
import dev.vality.wallets.hooker.converter.WebHookModelToWebHookConverter;
import dev.vality.wallets.hooker.converter.WebHookParamsToWebHookConverter;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import dev.vality.wallets.hooker.domain.WebHookModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebHookerService implements WebhookManagerSrv.Iface {

    private final WebHookDao webHookDao;
    private final WebHookConverter webHookConverter;
    private final WebHookParamsToWebHookConverter webHookParamsToWebHookConverter;
    private final WebHookModelToWebHookConverter webHookModelToWebHookConverter;

    @Override
    public List<Webhook> getList(String partyId) {
        log.info("Start get webhooks, partyId={}", partyId);

        List<Webhook> webhooks = webHookDao.getByParty(partyId).stream()
                .map(webHookConverter::convert)
                .collect(Collectors.toList());

        log.info("Finish get webhooks, partyId={}, size={}", partyId, webhooks.size());

        return webhooks;
    }

    @Override
    public Webhook get(long id) throws WebhookNotFound {
        log.info("Start get Webhook, id={}", id);

        WebHookModel webHookModel = webHookDao.getById(id);

        if (webHookModel == null) {
            log.warn("Webhook not found, {}", id);
            throw new WebhookNotFound();
        }

        Webhook webhook = webHookModelToWebHookConverter.convert(webHookModel);

        log.info("Finish get Webhook, webhook={}", webhook);

        return webhook;
    }

    @Override
    public Webhook create(WebhookParams webhookParams) {
        try {
            log.info("Start create webhook, webhookParams={}", webhookParams);

            WebHookModel webHookModel = webHookParamsToWebHookConverter.convert(webhookParams);

            var webhook = webHookDao.create(webHookModel);

            Webhook webhookResult = webHookConverter.convert(webhook);

            log.info("Finish create webhook, webhook={}", webhook);

            return webhookResult;
        } catch (Exception e) {
            log.error("Error when create webhook, {} ", webhookParams, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(long id) throws WebhookNotFound {
        try {
            log.info("Start delete webhook, id={}", id);

            webHookDao.delete(id);

            log.info("Finish delete webhook, id={}", id);
        } catch (Exception e) {
            log.error("Fail to delete webhook, {}", id, e);
            throw new WebhookNotFound();
        }
    }
}
