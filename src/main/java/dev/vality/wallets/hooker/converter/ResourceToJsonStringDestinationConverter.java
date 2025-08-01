package dev.vality.wallets.hooker.converter;

import com.google.common.base.CaseFormat;
import dev.vality.fistful.base.*;
import dev.vality.swag.wallets.webhook.events.model.BankCard;
import dev.vality.swag.wallets.webhook.events.model.CryptoWallet;
import dev.vality.swag.wallets.webhook.events.model.DigitalWallet;
import dev.vality.swag.wallets.webhook.events.model.*;
import dev.vality.wallets.hooker.exception.UnknownResourceException;
import dev.vality.wallets.hooker.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceToJsonStringDestinationConverter implements Converter<Resource, String> {

    @Override
    public String convert(Resource resource) {
        switch (resource.getSetField()) {
            case BANK_CARD:
                BankCard bankCard = new BankCard();
                ResourceBankCard resourceBankCard = resource.getBankCard();
                bankCard.setType(DestinationResource.TypeEnum.BANK_CARD);
                bankCard.bin(resourceBankCard.getBankCard().getBin());
                bankCard.cardNumberMask(resourceBankCard.getBankCard().getMaskedPan());
                if (resourceBankCard.getBankCard().getPaymentSystem() != null) {
                    bankCard.paymentSystem(resourceBankCard.getBankCard().getPaymentSystem().getId());
                }
                return JsonUtil.toString(bankCard);
            case CRYPTO_WALLET:
                CryptoWallet cryptoWallet = new CryptoWallet();
                cryptoWallet.setType(DestinationResource.TypeEnum.CRYPTO_WALLET);
                ResourceCryptoWallet resourceCryptoWallet = resource.getCryptoWallet();
                cryptoWallet.setCryptoWalletId(resourceCryptoWallet.getCryptoWallet().getId());
                cryptoWallet.setCurrency(
                        CryptoCurrency.fromValue(
                                CaseFormat.UPPER_UNDERSCORE.to(
                                        CaseFormat.UPPER_CAMEL,
                                        resourceCryptoWallet.getCryptoWallet().getCurrency().getId())));
                return JsonUtil.toString(cryptoWallet);
            case DIGITAL_WALLET:
                DigitalWallet digitalWallet = new DigitalWallet();
                var swagDigitalWallet = resource.getDigitalWallet().getDigitalWallet();
                digitalWallet.setDigitalWalletId(swagDigitalWallet.getId());
                digitalWallet.setDigitalWalletProvider(swagDigitalWallet.getPaymentService().getId());
                digitalWallet.setType(DestinationResource.TypeEnum.DIGITAL_WALLET);
                return JsonUtil.toString(digitalWallet);
            case GENERIC:
                return initGenericType(resource);
            default:
                throw new UnknownResourceException("Can't init destination with unknown resource");
        }
    }

    private String initGenericType(Resource resource) {
        ResourceGenericData generic = resource.getGeneric().getGeneric();
        Content content = generic.getData();
        return new String(content.getData());
    }

}
