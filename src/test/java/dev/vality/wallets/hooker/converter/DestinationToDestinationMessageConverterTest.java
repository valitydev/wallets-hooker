package dev.vality.wallets.hooker.converter;

import dev.vality.fistful.base.*;
import dev.vality.fistful.destination.Destination;
import dev.vality.fistful.msgpack.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DestinationToDestinationMessageConverterTest {

    private DestinationToDestinationMessageConverter converter = new DestinationToDestinationMessageConverter();
    private ResourceToJsonStringDestinationConverter resourceToJsonStringDestinationConverter
            = new ResourceToJsonStringDestinationConverter();

    @Test
    public void testConvertFromEventWithBankCardResource() {
        Resource resource = Resource.bank_card(
                new ResourceBankCard(
                        new BankCard("token")
                                .setBin("bin")
                                .setMaskedPan("masked_pan")
                                .setCardType(CardType.charge_card)
                                .setBinDataId(Value.i(1))
                                .setPaymentSystem(new PaymentSystemRef(
                                        LegacyBankCardPaymentSystem.mastercard.name()))
                )
        );
        Destination destination = new Destination()
                .setName("name")
                .setExternalId("external_id")
                .setResource(resource);
        var swagDestination = converter.convert(destination);
        assertEquals(destination.getExternalId(), swagDestination.getExternalID());
        assertEquals(destination.getName(), swagDestination.getName());

        String resultString = resourceToJsonStringDestinationConverter.convert(resource);
        assertEquals("{\"type\":\"BankCard\",\"cardNumberMask\":\"masked_pan\",\"bin\":\"bin\"," +
                "\"lastDigits\":null,\"paymentSystem\":\"mastercard\",\"tokenProvider\":null}", resultString);
    }

    @Test
    public void testConvertFromEventWithCryptoWalletResource() {
        Resource resource = Resource.crypto_wallet(
                new ResourceCryptoWallet(
                        new CryptoWallet("crypto_wallet_id", CryptoCurrency.bitcoin_cash)
                                .setData(CryptoData.bitcoin_cash(new CryptoDataBitcoinCash()))
                )
        );
        Destination destination = new Destination()
                .setName("name")
                .setExternalId("external_id")
                .setResource(resource);
        var swagDestination = converter.convert(destination);
        assertEquals(destination.getExternalId(), swagDestination.getExternalID());
        assertEquals(destination.getName(), swagDestination.getName());

        String resultString = resourceToJsonStringDestinationConverter.convert(resource);
        assertEquals("{\"type\":\"CryptoWallet\",\"cryptoWalletId\":\"crypto_wallet_id\"," +
                "\"currency\":\"BitcoinCash\"}", resultString);
    }

    @Test
    public void testConvertFromEventWithDigitalWalletResource() {
        Resource digitalWalletId = Resource.digital_wallet(
                new ResourceDigitalWallet((
                        new DigitalWallet("digital_wallet_id", new PaymentServiceRef().setId("123"))
                ))
        );
        Destination destination = new Destination()
                .setName("name")
                .setExternalId("external_id")
                .setResource(digitalWalletId);
        var swagDestination = converter.convert(destination);
        assertEquals(destination.getExternalId(), swagDestination.getExternalID());
        assertEquals(destination.getName(), swagDestination.getName());

        String digitalWalletString = resourceToJsonStringDestinationConverter.convert(digitalWalletId);
        assertEquals("{\"type\":\"DigitalWallet\",\"digitalWalletId\":\"digital_wallet_id\"," +
                "\"digitalWalletProvider\":\"123\"}", digitalWalletString);
    }

    @Test
    public void testConvertFromEventWithBankCardResourceAndPaymentSystemIsNull() {
        Resource resource = Resource.bank_card(
                new ResourceBankCard(
                        new BankCard("token")
                                .setBin("bin")
                                .setMaskedPan("masked_pan")
                                .setCardType(CardType.charge_card)
                                .setBinDataId(Value.i(1))
                                .setPaymentSystem(null)
                )
        );
        Destination destination = new Destination()
                .setName("name")
                .setExternalId("external_id")
                .setResource(resource);
        var swagDestination = converter.convert(destination);
        assertEquals(destination.getExternalId(), swagDestination.getExternalID());
        assertEquals(destination.getName(), swagDestination.getName());

        String resultString = resourceToJsonStringDestinationConverter.convert(resource);
        assertEquals("{\"type\":\"BankCard\",\"cardNumberMask\":\"masked_pan\",\"bin\":\"bin\"," +
                "\"lastDigits\":null,\"paymentSystem\":null,\"tokenProvider\":null}", resultString);
    }

}
