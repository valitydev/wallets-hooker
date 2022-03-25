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
        // todo metadata null?
        destination.setMetadata(null);

        log.info("destinationDamsel has been converted, destination={}", destination);
        return destination;
    }

}
