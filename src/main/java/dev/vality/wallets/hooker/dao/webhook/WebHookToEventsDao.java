package dev.vality.wallets.hooker.dao.webhook;

import dev.vality.wallets.hooker.domain.tables.pojos.WebhookToEvents;

import java.util.List;

public interface WebHookToEventsDao {

    void create(WebhookToEvents webhookToEvents);

    List<WebhookToEvents> get(long id);

}
