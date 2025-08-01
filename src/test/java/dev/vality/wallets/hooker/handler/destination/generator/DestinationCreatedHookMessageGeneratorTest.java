package dev.vality.wallets.hooker.handler.destination.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.swag.wallets.webhook.events.model.BankCard;
import dev.vality.swag.wallets.webhook.events.model.Destination;
import dev.vality.swag.wallets.webhook.events.model.DestinationCreated;
import dev.vality.swag.wallets.webhook.events.model.DestinationResource;
import dev.vality.wallets.hooker.config.ObjectMapperConfig;
import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.handler.AdditionalHeadersGenerator;
import dev.vality.wallets.hooker.model.MessageGenParams;
import dev.vality.wallets.hooker.service.WebHookMessageGeneratorServiceImpl;
import dev.vality.wallets.hooker.service.crypt.AsymSigner;
import dev.vality.wallets.hooker.service.crypt.KeyPair;
import dev.vality.wallets.hooker.service.crypt.Signer;
import dev.vality.wallets.hooker.domain.enums.EventType;
import dev.vality.wallets.hooker.domain.tables.pojos.DestinationMessage;
import dev.vality.webhook.dispatcher.WebhookMessage;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DestinationCreatedHookMessageGeneratorTest {

    public static final long EVENT_ID = 1L;
    public static final String URL = "/url";
    public static final String WALLET_ID = "wallet_id";
    public static final String PARTY_ID = "party_id";
    public static final String SOURCE_ID = "sourceId";
    public static final String DESTINATION_ID = "destination_id";

    ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    Signer signer = new AsymSigner();

    WebHookMessageGeneratorServiceImpl<DestinationMessage> generatorService =
            new WebHookMessageGeneratorServiceImpl<>(-1L);
    DestinationCreatedHookMessageGenerator destinationCreatedHookMessageGenerator =
            new DestinationCreatedHookMessageGenerator(
                    generatorService,
                    objectMapper,
                    new AdditionalHeadersGenerator(signer),
                    -1L);

    @Test
    void generate() throws IOException {
        WebHookModel model = new WebHookModel();
        model.setId(1L);
        model.setEventTypes(Set.of(EventType.DESTINATION_CREATED));
        model.setEnabled(true);
        model.setPartyId(PARTY_ID);
        model.setUrl(URL);
        model.setWalletId(WALLET_ID);

        KeyPair keyPair = signer.generateKeys();
        model.setPrivateKey(keyPair.getPrivKey());
        model.setPubKey(keyPair.getPublKey());

        Destination destination = new Destination();
        destination.setId(DESTINATION_ID);
        BankCard bankCard = new BankCard();
        bankCard.setBin("123456");
        bankCard.setCardNumberMask("1234*******6789");
        bankCard.setLastDigits("1234");
        bankCard.setType(DestinationResource.TypeEnum.BANK_CARD);
        bankCard.setPaymentSystem("visa");
        destination.setResource(bankCard);
        destination.setCurrency("RUB");

        DestinationMessage event = new DestinationMessage();
        event.setMessage(objectMapper.writeValueAsString(destination));
        event.setDestinationId(DESTINATION_ID);

        String createdAt = "2019-07-02T08:43:42Z";

        MessageGenParams genParam = MessageGenParams.builder()
                .sourceId(SOURCE_ID)
                .eventId(EVENT_ID)
                .parentId(0L)
                .createdAt(createdAt)
                .build();
        WebhookMessage generate = destinationCreatedHookMessageGenerator.generate(event, model, genParam);

        System.out.println(generate);

        assertEquals(EVENT_ID, generate.getEventId());
        assertEquals(URL, generate.getUrl());
        assertEquals(ContentType.APPLICATION_JSON.getMimeType(), generate.getContentType());
        assertEquals(SOURCE_ID, generate.getSourceId());
        assertNotNull(generate.getAdditionalHeaders().get(AdditionalHeadersGenerator.SIGNATURE_HEADER));
        byte[] requestBody = generate.getRequestBody();

        DestinationCreated value = objectMapper.readValue(requestBody, DestinationCreated.class);
        assertNotNull(PARTY_ID, value.getDestination().getParty());
        assertEquals(DestinationResource.TypeEnum.BANK_CARD, value.getDestination().getResource().getType());
        assertEquals(DESTINATION_ID, value.getDestination().getId());
    }
}
