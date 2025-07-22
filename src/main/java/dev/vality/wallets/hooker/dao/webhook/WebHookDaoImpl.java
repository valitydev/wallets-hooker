package dev.vality.wallets.hooker.dao.webhook;

import com.zaxxer.hikari.HikariDataSource;
import dev.vality.mapper.RecordRowMapper;
import dev.vality.wallets.hooker.dao.AbstractDao;
import dev.vality.wallets.hooker.dao.party.PartyKeyDao;
import dev.vality.wallets.hooker.dao.webhook.mapper.WebHookModelRowMapper;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.domain.tables.pojos.PartyKey;
import dev.vality.wallets.hooker.domain.tables.pojos.Webhook;
import dev.vality.wallets.hooker.domain.tables.pojos.WebhookToEvents;
import dev.vality.wallets.hooker.domain.tables.records.WebhookRecord;
import dev.vality.wallets.hooker.service.crypt.KeyPair;
import dev.vality.wallets.hooker.service.crypt.Signer;
import dev.vality.wallets.hooker.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Query;
import org.jooq.Record7;
import org.jooq.SelectOnConditionStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static dev.vality.wallets.hooker.domain.tables.PartyKey.PARTY_KEY;
import static dev.vality.wallets.hooker.domain.tables.Webhook.WEBHOOK;
import static dev.vality.wallets.hooker.domain.tables.WebhookToEvents.WEBHOOK_TO_EVENTS;

@Component
@Slf4j
public class WebHookDaoImpl extends AbstractDao implements WebHookDao {

    private static final int LIMIT = 1000;
    private final RowMapper<Webhook> webhookRowMapper;
    private final RowMapper<WebHookModel> webHookModelRowMapper;
    private final WebHookToEventsDao webHookToEventsDao;
    private final PartyKeyDao partyKeyDao;
    private final Signer signer;

    @Autowired
    public WebHookDaoImpl(HikariDataSource dataSource,
                          WebHookToEventsDao webHookToEventsDao,
                          PartyKeyDao partyKeyDao,
                          Signer signer) {
        super(dataSource);
        this.webHookToEventsDao = webHookToEventsDao;
        this.signer = signer;
        this.webhookRowMapper = new RecordRowMapper<>(WEBHOOK, Webhook.class);
        this.webHookModelRowMapper = new WebHookModelRowMapper();
        this.partyKeyDao = partyKeyDao;
    }

    @Override
    public Webhook create(WebHookModel webHookModel) {
        String partyId = webHookModel.getPartyId();

        var partyKey = partyKeyDao.getByParty(partyId);

        if (partyKey == null) {
            partyKey = new PartyKey();
            partyKey.setPartyId(partyId);
            KeyPair keyPair = signer.generateKeys();
            partyKey.setPubKey(keyPair.getPublKey());
            partyKey.setPrivKey(keyPair.getPrivKey());

            partyKeyDao.create(partyKey);
        }

        WebhookRecord record = getDslContext().newRecord(WEBHOOK, webHookModel);
        Query query = getDslContext()
                .insertInto(WEBHOOK)
                .set(record)
                .onConflict(WEBHOOK.ID)
                .doNothing()
                .returning(
                        WEBHOOK.ID,
                        WEBHOOK.PARTY_ID,
                        WEBHOOK.ENABLED,
                        WEBHOOK.URL,
                        WEBHOOK.WALLET_ID
                );

        Webhook webhook = fetchOne(query, webhookRowMapper);

        webHookModel.getEventTypes().forEach(
                eventType -> webHookToEventsDao.create(new WebhookToEvents(webhook.getId(), eventType))
        );

        log.info("webhook has been created, webHookModel={} ", webHookModel.toString());

        return webhook;
    }

    @Override
    public void delete(long id) {
        Query query = getDslContext().delete(WEBHOOK_TO_EVENTS).where(WEBHOOK_TO_EVENTS.HOOK_ID.eq(id));
        execute(query);

        query = getDslContext().delete(WEBHOOK).where(WEBHOOK.ID.eq(id));
        execute(query);

        log.info("webhook has been deleted, id={} ", id);
    }

    @Override
    public WebHookModel getById(long id) {
        Query query = getDslContext()
                .select(
                        WEBHOOK.ID,
                        WEBHOOK.PARTY_ID,
                        WEBHOOK.ENABLED,
                        WEBHOOK.URL,
                        WEBHOOK.WALLET_ID,
                        PARTY_KEY.PUB_KEY,
                        PARTY_KEY.PRIV_KEY
                )
                .from(WEBHOOK)
                .leftJoin(PARTY_KEY).on(WEBHOOK.PARTY_ID.eq(PARTY_KEY.PARTY_ID))
                .where(WEBHOOK.ID.eq(id));

        WebHookModel webHookModel = fetchOne(query, webHookModelRowMapper);

        if (webHookModel != null) {
            webHookModel.setEventTypes(
                    webHookToEventsDao.get(id).stream()
                            .map(WebhookToEvents::getEventType)
                            .collect(Collectors.toSet())
            );

            log.info("webhook has been got, webHookModel={} ", webHookModel);
        }

        return webHookModel;
    }

    private SelectOnConditionStep<
            Record7<Long, String, Boolean, String, String, String, String>> createSelectForWebHookModel() {
        return getDslContext()
                .select(
                        WEBHOOK.ID,
                        WEBHOOK.PARTY_ID,
                        WEBHOOK.ENABLED,
                        WEBHOOK.URL,
                        WEBHOOK.WALLET_ID,
                        PARTY_KEY.PUB_KEY,
                        PARTY_KEY.PRIV_KEY
                )
                .from(WEBHOOK)
                .leftJoin(WEBHOOK_TO_EVENTS).on(WEBHOOK.ID.eq(WEBHOOK_TO_EVENTS.HOOK_ID))
                .leftJoin(PARTY_KEY).on(WEBHOOK.PARTY_ID.eq(PARTY_KEY.PARTY_ID));
    }

    @Override
    public List<Webhook> getByParty(String partyId) {
        Query query = getDslContext()
                .selectFrom(WEBHOOK)
                .where(WEBHOOK.PARTY_ID.eq(partyId))
                .limit(LIMIT);
        return getSafeWebHook(query, () -> log.info("webhooks has been got, partyId={}", partyId));
    }

    @Override
    public List<WebHookModel> getByPartyAndEventType(String partyId, EventType eventType) {
        Query query = createSelectForWebHookModel()
                .where(WEBHOOK.PARTY_ID.eq(partyId)
                        .and(WEBHOOK_TO_EVENTS.EVENT_TYPE.eq(eventType)))
                .limit(LIMIT);
        return getWebHookModels(query);
    }

    private List<WebHookModel> getWebHookModels(Query query) {
        List<WebHookModel> webHookModels = fetch(query, webHookModelRowMapper);
        List<WebHookModel> webHookModelsSafe = webHookModels == null ? Collections.emptyList() : webHookModels;
        if (!webHookModelsSafe.isEmpty()) {
            webHookModelsSafe.forEach(
                    webHookModel -> webHookModel.setEventTypes(
                            webHookToEventsDao.get(webHookModel.getId()).stream()
                                    .map(WebhookToEvents::getEventType)
                                    .collect(Collectors.toSet())
                    )
            );
            log.info("webhooks has been got, webHookModels={}", LogUtils.getLogWebHookModel(webHookModelsSafe));
        }
        return webHookModelsSafe;
    }

    private List<Webhook> getSafeWebHook(Query query, Runnable runIfNotEmpty) {
        List<Webhook> webhooks = fetch(query, webhookRowMapper);
        List<Webhook> webhooksSafe = webhooks == null ? Collections.emptyList() : webhooks;
        if (!webhooksSafe.isEmpty()) {
            runIfNotEmpty.run();
        }
        return webhooksSafe;
    }
}
