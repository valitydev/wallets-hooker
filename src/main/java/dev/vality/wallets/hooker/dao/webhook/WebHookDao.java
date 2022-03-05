package dev.vality.wallets.hooker.dao.webhook;

import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.domain.tables.pojos.Webhook;

import java.util.List;

public interface WebHookDao {

    Webhook create(WebHookModel webhook);

    void delete(long id);

    WebHookModel getById(long id);

    List<WebHookModel> getModelByIdentityAndWalletId(String identityId, String walletId, EventType eventType);

    List<Webhook> getByIdentity(String identityId);

    List<WebHookModel> getByIdentityAndEventType(String identityId, EventType eventType);
}
