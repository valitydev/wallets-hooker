package dev.vality.wallets.hooker.converter;

import com.google.common.base.CaseFormat;
import dev.vality.fistful.base.CryptoData;
import dev.vality.fistful.base.Resource;
import dev.vality.fistful.base.ResourceBankCard;
import dev.vality.fistful.base.ResourceCryptoWallet;
import dev.vality.fistful.destination.Destination;
import dev.vality.mamsel.PaymentSystemUtil;
import dev.vality.swag.wallets.webhook.events.model.*;
import dev.vality.wallets.hooker.exception.UnknownResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DestinationToDestinationMessageConverter
        implements Converter<Destination, dev.vality.swag.wallets.webhook.events.model.Destination> {

    @Override
    public dev.vality.swag.wallets.webhook.events.model.Destination convert(Destination event) {
        var destination = new dev.vality.swag.wallets.webhook.events.model.Destination();
        destination.setExternalID(event.getExternalId());
        destination.setName(event.getName());
        DestinationResource destinationResource = initDestinationResource(event.getResource());
        destination.setResource(destinationResource);
        // todo metadata null?
        destination.setMetadata(null);

        log.info("destinationDamsel has been converted, destination={}", destination);
        return destination;
    }

    private DestinationResource initDestinationResource(Resource resource) {
        switch (resource.getSetField()) {
            case BANK_CARD:
                BankCard bankCard = new BankCard();
                ResourceBankCard resourceBankCard = resource.getBankCard();
                bankCard.setType(DestinationResource.TypeEnum.BANKCARD);
                bankCard.bin(resourceBankCard.getBankCard().getBin());
                bankCard.cardNumberMask(resourceBankCard.getBankCard().getMaskedPan());
                bankCard.paymentSystem(PaymentSystemUtil.getFistfulPaymentSystemName(resourceBankCard.getBankCard()));
                return bankCard;
            case CRYPTO_WALLET:
                CryptoWallet cryptoWallet = new CryptoWallet();
                cryptoWallet.setType(DestinationResource.TypeEnum.CRYPTOWALLET);
                ResourceCryptoWallet resourceCryptoWallet = resource.getCryptoWallet();
                cryptoWallet.setCryptoWalletId(resourceCryptoWallet.getCryptoWallet().id);
                if (resourceCryptoWallet.getCryptoWallet().isSetData()) {
                    CryptoData cryptoData = resourceCryptoWallet.getCryptoWallet().getData();
                    cryptoWallet.setCurrency(
                            CryptoCurrency.fromValue(
                                    CaseFormat.UPPER_UNDERSCORE.to(
                                            CaseFormat.UPPER_CAMEL,
                                            cryptoData.getSetField().getFieldName()
                                    )
                            )
                    );
                }
                return cryptoWallet;
            case DIGITAL_WALLET:
                DigitalWallet digitalWallet = new DigitalWallet();
                var swagDigitalWallet = resource.getDigitalWallet().getDigitalWallet();
                digitalWallet.setDigitalWalletId(swagDigitalWallet.getId());
                digitalWallet.setDigitalWalletProvider(swagDigitalWallet.getPaymentService().getId());
                digitalWallet.setType(DestinationResource.TypeEnum.DIGITALWALLET);
                return digitalWallet;
            default:
                throw new UnknownResourceException("Can't init destination with unknown resource");
        }
    }
}
