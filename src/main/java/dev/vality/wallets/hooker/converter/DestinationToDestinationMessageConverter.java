package dev.vality.wallets.hooker.converter;

import dev.vality.fistful.destination.Destination;
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
        destination.setParty(event.getPartyId());
        // todo metadata null?
        destination.setMetadata(null);
        log.info("destinationDamsel has been converted, destination={}", destination);
        return destination;
    }
}
