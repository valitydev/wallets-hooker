package dev.vality.wallets.hooker.dao.webhook;

import dev.vality.wallets.hooker.config.PostgresqlSpringBootITest;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.domain.tables.pojos.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@PostgresqlSpringBootITest
public class WebHookDaoImplTest {

    public static final String IDENTITY_ID = "123";
    public static final String WALLET_123 = "wallet_123";

    @Autowired
    private WebHookDao webHookDao;

    @Test
    public void create() {
        LinkedHashSet<EventType> eventTypes = new LinkedHashSet<>();
        eventTypes.add(EventType.WITHDRAWAL_CREATED);
        eventTypes.add(EventType.WITHDRAWAL_SUCCEEDED);
        WebHookModel webhook = WebHookModel.builder()
                .enabled(true)
                .identityId(IDENTITY_ID)
                .url("/qwe")
                .walletId(WALLET_123)
                .eventTypes(eventTypes)
                .build();
        Webhook webhook1 = webHookDao.create(webhook);

        WebHookModel webHookModel = webHookDao.getById(webhook1.getId());

        assertEquals(IDENTITY_ID, webHookModel.getIdentityId());
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
                .identityId(IDENTITY_ID)
                .url("/qwe")
                .walletId(null)
                .eventTypes(eventTypes)
                .build();
        Webhook webhook2 = webHookDao.create(webhook);

        List<WebHookModel> modelByIdentityAndWalletId =
                webHookDao.getByIdentityAndEventType(IDENTITY_ID, EventType.DESTINATION_CREATED);

        assertEquals(1, modelByIdentityAndWalletId.size());
        assertNotNull(modelByIdentityAndWalletId.get(0).getEventTypes());

        webHookDao.create(webhook);
        modelByIdentityAndWalletId = webHookDao.getByIdentityAndEventType(IDENTITY_ID, EventType.DESTINATION_CREATED);
        assertEquals(2, modelByIdentityAndWalletId.size());
        assertNotNull(modelByIdentityAndWalletId.get(0).getEventTypes());

        modelByIdentityAndWalletId =
                webHookDao.getModelByIdentityAndWalletId(IDENTITY_ID, WALLET_123, EventType.DESTINATION_CREATED);

        assertEquals(0, modelByIdentityAndWalletId.size());
    }
}