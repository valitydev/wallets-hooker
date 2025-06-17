package dev.vality.wallets.hooker.dao.webhook;

import dev.vality.wallets.hooker.config.PostgresqlSpringBootITest;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.domain.tables.pojos.Webhook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@PostgresqlSpringBootITest
public class WebHookDaoImplTest {

    public static final String PARTY_ID = "123";
    public static final String WALLET_123 = "wallet_123";

    @Autowired
    private WebHookDao webHookDao;

    @Test
    void create() {
        LinkedHashSet<EventType> eventTypes = new LinkedHashSet<>();
        eventTypes.add(EventType.WITHDRAWAL_CREATED);
        eventTypes.add(EventType.WITHDRAWAL_SUCCEEDED);
        WebHookModel webhook = WebHookModel.builder()
                .enabled(true)
                .partyId(PARTY_ID)
                .url("/qwe")
                .walletId(WALLET_123)
                .eventTypes(eventTypes)
                .build();
        Webhook webhook1 = webHookDao.create(webhook);

        WebHookModel webHookModel = webHookDao.getById(webhook1.getId());

        assertEquals(PARTY_ID, webHookModel.getPartyId());
        assertEquals(WALLET_123, webHookModel.getWalletId());
        assertEquals(eventTypes, webHookModel.getEventTypes());
        assertFalse(webHookModel.getPubKey().isEmpty());
        assertNotNull(webHookModel.getEventTypes());

        webHookDao.delete(webhook1.getId());

        webHookModel = webHookDao.getById(webhook1.getId());

        assertNull(webHookModel);

        eventTypes = new LinkedHashSet<>();
        eventTypes.add(EventType.DESTINATION_CREATED);
        eventTypes.add(EventType.DESTINATION_AUTHORIZED);
        webhook = WebHookModel.builder()
                .enabled(true)
                .partyId(PARTY_ID)
                .url("/qwe")
                .walletId(null)
                .eventTypes(eventTypes)
                .build();
        Webhook webhook2 = webHookDao.create(webhook);

        List<WebHookModel> modelByPartyAndWalletId =
                webHookDao.getByPartyAndEventType(PARTY_ID, EventType.DESTINATION_CREATED);

        assertEquals(1, modelByPartyAndWalletId.size());
        assertNotNull(modelByPartyAndWalletId.get(0).getEventTypes());

        webHookDao.create(webhook);
        modelByPartyAndWalletId = webHookDao.getByPartyAndEventType(PARTY_ID, EventType.DESTINATION_CREATED);
        assertEquals(2, modelByPartyAndWalletId.size());
        assertNotNull(modelByPartyAndWalletId.get(0).getEventTypes());
    }
}