package dev.vality.wallets.hooker.converter;

import dev.vality.fistful.base.*;
import dev.vality.fistful.destination.Destination;
import dev.vality.fistful.msgpack.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DestinationToDestinationMessageConverterTest {

    private DestinationToDestinationMessageConverter converter = new DestinationToDestinationMessageConverter();

    @Test
    public void testConvertFromEventWithBankCardResource() {
        Destination destination = new Destination()
                .setName("name")
                .setExternalId("external_id")
                .setResource(Resource.bank_card(
                        new ResourceBankCard(
                                new BankCard("token")
                                        .setBin("bin")
                                        .setMaskedPan("masked_pan")
                                        .setCardType(CardType.charge_card)
                                        .setBinDataId(Value.i(1))
                                        .setPaymentSystem(new PaymentSystemRef(
                                                LegacyBankCardPaymentSystem.mastercard.name()))
                        )
                ));
        var swagDestination = converter.convert(destination);
        assertEquals(destination.getExternalId(), swagDestination.getExternalID());
        assertEquals(destination.getName(), swagDestination.getName());
        var bankCard = (dev.vality.swag.wallets.webhook.events.model.BankCard) swagDestination.getResource();
        assertNotNull(bankCard.getPaymentSystem());
    }

    @Test
    public void testConvertFromEventWithCryptoWalletResource() {
        Destination destination = new Destination()
                .setName("name")
                .setExternalId("external_id")
                .setResource(Resource.crypto_wallet(
                        new ResourceCryptoWallet(
                                new CryptoWallet("crypto_wallet_id", CryptoCurrency.bitcoin_cash)
                                        .setData(CryptoData.bitcoin_cash(new CryptoDataBitcoinCash()))
                        )
                ));
        var swagDestination = converter.convert(destination);
        assertEquals(destination.getExternalId(), swagDestination.getExternalID());
        assertEquals(destination.getName(), swagDestination.getName());
        var cryptoWallet = (dev.vality.swag.wallets.webhook.events.model.CryptoWallet) swagDestination.getResource();
        assertNotNull(cryptoWallet.getCurrency());
    }

    @Test
    public void testConvertFromEventWithDigitalWalletResource() {
        Destination destination = new Destination()
                .setName("name")
                .setExternalId("external_id")
                .setResource(Resource.digital_wallet(
                        new ResourceDigitalWallet((
                                new DigitalWallet("digital_wallet_id", new PaymentServiceRef().setId("123"))
                        ))
                ));
        var swagDestination = converter.convert(destination);
        assertEquals(destination.getExternalId(), swagDestination.getExternalID());
        assertEquals(destination.getName(), swagDestination.getName());
        var digitalWallet = (dev.vality.swag.wallets.webhook.events.model.DigitalWallet) swagDestination
                .getResource();
        assertNotNull(digitalWallet.getDigitalWalletId());
        assertNotNull(digitalWallet.getDigitalWalletProvider());
    }

    @Test
    public void testConvertFromEventWithBankCardResourceAndPaymentSystemIsNull() {
        Destination destination = new Destination()
                .setName("name")
                .setExternalId("external_id")
                .setResource(Resource.bank_card(
                        new ResourceBankCard(
                                new BankCard("token")
                                        .setBin("bin")
                                        .setMaskedPan("masked_pan")
                                        .setCardType(CardType.charge_card)
                                        .setBinDataId(Value.i(1))
                                        .setPaymentSystem(null)
                        )
                ));
        var swagDestination = converter.convert(destination);
        assertEquals(destination.getExternalId(), swagDestination.getExternalID());
        assertEquals(destination.getName(), swagDestination.getName());
        var bankCard = (dev.vality.swag.wallets.webhook.events.model.BankCard) swagDestination.getResource();
        assertNull(bankCard.getPaymentSystem());
    }

}
