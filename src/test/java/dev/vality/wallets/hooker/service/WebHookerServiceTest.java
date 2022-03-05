package dev.vality.wallets.hooker.service;

import dev.vality.fistful.webhooker.Webhook;
import dev.vality.wallets.hooker.converter.WebHookConverter;
import dev.vality.wallets.hooker.converter.WebHookModelToWebHookConverter;
import dev.vality.wallets.hooker.converter.WebHookParamsToWebHookConverter;
import dev.vality.wallets.hooker.dao.webhook.WebHookDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class WebHookerServiceTest {

    @Mock
    private WebHookDao webHookDao;

    @Mock
    private WebHookConverter webHookConverter;

    @Mock
    private WebHookParamsToWebHookConverter webHookParamsToWebHookConverter;

    @Mock
    private WebHookModelToWebHookConverter webHookModelToWebHookConverter;

    private WebHookerService webHookerService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        webHookerService = new WebHookerService(webHookDao, webHookConverter, webHookParamsToWebHookConverter,
                webHookModelToWebHookConverter);
    }

    @Test
    public void getList() {
        String id = "test";
        ArrayList<dev.vality.wallets.hooker.domain.tables.pojos.Webhook> webhooks = new ArrayList<>();
        dev.vality.wallets.hooker.domain.tables.pojos.Webhook webhook =
                new dev.vality.wallets.hooker.domain.tables.pojos.Webhook();
        webhook.setIdentityId(id);
        webhooks.add(webhook);
        Mockito.when(webHookDao.getByIdentity(id)).thenReturn(webhooks);
        Webhook hook = new Webhook();
        hook.setIdentityId(id);
        Mockito.when(webHookConverter.convert(webhook)).thenReturn(hook);
        List<Webhook> listWebHooks = webHookerService.getList(id);

        assertFalse(listWebHooks.isEmpty());
        assertEquals(id, listWebHooks.get(0).identity_id);
    }

}