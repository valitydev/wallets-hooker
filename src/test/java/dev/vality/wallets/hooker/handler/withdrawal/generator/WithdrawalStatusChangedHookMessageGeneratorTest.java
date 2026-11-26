package dev.vality.wallets.hooker.handler.withdrawal.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.fistful.base.Failure;
import dev.vality.fistful.base.SubFailure;
import dev.vality.fistful.withdrawal.Change;
import dev.vality.fistful.withdrawal.StatusChange;
import dev.vality.fistful.withdrawal.TimestampedChange;
import dev.vality.fistful.withdrawal.status.Failed;
import dev.vality.fistful.withdrawal.status.Status;
import dev.vality.fistful.withdrawal.status.Succeeded;
import dev.vality.wallets.hooker.config.ObjectMapperConfig;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.handler.AdditionalHeadersGenerator;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.wallets.hooker.service.WebHookMessageGeneratorServiceImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class WithdrawalStatusChangedHookMessageGeneratorTest {

    private static final String CREATED_AT = "2026-01-01T00:00:00Z";

    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    private final WithdrawalStatusChangedHookMessageGenerator generator =
            new WithdrawalStatusChangedHookMessageGenerator(
                    new WebHookMessageGeneratorServiceImpl<>(-1L),
                    objectMapper,
                    mock(AdditionalHeadersGenerator.class),
                    -1L);

    @Test
    void includesFailureDetailsInFailedWebhook() throws IOException {
        var failure = new Failure("authorization_failed")
                .setReason("The issuer declined the withdrawal")
                .setSub(new SubFailure("declined").setSub(new SubFailure("insufficient_funds")));

        JsonNode body = generateBody(Status.failed(new Failed(failure)));

        assertEquals("WithdrawalFailed", body.path("eventType").asText());
        assertEquals("authorization_failed", body.at("/error/code").asText());
        assertEquals("The issuer declined the withdrawal", body.at("/error/message").asText());
        assertEquals("declined", body.at("/error/subError/code").asText());
        assertEquals("insufficient_funds", body.at("/error/subError/subError/code").asText());
    }

    @Test
    void usesFailureCodeWhenReasonIsMissing() throws IOException {
        JsonNode body = generateBody(Status.failed(new Failed(new Failure("declined"))));

        assertEquals("declined", body.at("/error/message").asText());
        assertFalse(body.path("error").has("subError"));
    }

    @Test
    void omitsErrorWhenFailureIsMissing() throws IOException {
        JsonNode body = generateBody(Status.failed(new Failed()));

        assertEquals("WithdrawalFailed", body.path("eventType").asText());
        assertFalse(body.has("error"));
    }

    @Test
    void omitsErrorForSuccessfulWithdrawal() throws IOException {
        JsonNode body = generateBody(Status.succeeded(new Succeeded()));

        assertEquals("WithdrawalSucceeded", body.path("eventType").asText());
        assertFalse(body.has("error"));
    }

    private JsonNode generateBody(Status status) throws IOException {
        var model = new WebHookModel();
        model.setId(1L);
        model.setUrl("https://example.com/webhook");
        var event = new TimestampedChange(CREATED_AT, Change.status_changed(new StatusChange(status)));
        var params = MessageGenParams.builder()
                .sourceId("withdrawal-id")
                .eventId(1L)
                .createdAt(CREATED_AT)
                .build();
        return objectMapper.readTree(generator.generate(event, model, params).getRequestBody());
    }
}
