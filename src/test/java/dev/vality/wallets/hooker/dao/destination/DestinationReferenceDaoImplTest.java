package dev.vality.wallets.hooker.dao.destination;

import dev.vality.wallets.hooker.config.PostgresqlSpringBootITest;
import dev.vality.wallets.hooker.domain.tables.pojos.DestinationIdentityReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

@PostgresqlSpringBootITest
public class DestinationReferenceDaoImplTest {

    @Autowired
    private DestinationReferenceDao destinationReferenceDao;

    @Test
    public void create() {
        DestinationIdentityReference reference = new DestinationIdentityReference();
        reference.setIdentityId("identity");
        reference.setEventId("eventId");
        String destination = "destination";
        reference.setDestinationId(destination);
        reference.setExternalId("externalId");
        destinationReferenceDao.create(reference);

        DestinationIdentityReference destinationIdentityReference = destinationReferenceDao.get(destination);

        assertEquals(reference, destinationIdentityReference);
    }
}
