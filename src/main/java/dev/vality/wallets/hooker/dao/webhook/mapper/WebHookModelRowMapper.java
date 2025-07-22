package dev.vality.wallets.hooker.dao.webhook.mapper;

import dev.vality.wallets.hooker.domain.WebHookModel;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static dev.vality.wallets.hooker.domain.tables.PartyKey.PARTY_KEY;
import static dev.vality.wallets.hooker.domain.tables.Webhook.WEBHOOK;

public class WebHookModelRowMapper implements RowMapper<WebHookModel> {

    @Override
    public WebHookModel mapRow(ResultSet rs, int i) throws SQLException {
        return WebHookModel.builder()
                .id(rs.getLong(WEBHOOK.ID.getName()))
                .partyId(rs.getString(WEBHOOK.PARTY_ID.getName()))
                .walletId(rs.getString(WEBHOOK.WALLET_ID.getName()))
                .eventTypes(null)
                .url(rs.getString(WEBHOOK.URL.getName()))
                .enabled(rs.getBoolean(WEBHOOK.ENABLED.getName()))
                .pubKey(rs.getString(PARTY_KEY.PUB_KEY.getName()))
                .privateKey(rs.getString(PARTY_KEY.PRIV_KEY.getName()))
                .build();
    }
}
