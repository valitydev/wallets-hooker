package dev.vality.wallets.hooker.handler;

import dev.vality.wallets.hooker.domain.WebHookModel;
import dev.vality.wallets.hooker.service.crypt.AsymSigner;
import dev.vality.wallets.hooker.service.crypt.KeyPair;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AdditionalHeadersGeneratorTest {

    AsymSigner signer = new AsymSigner();
    AdditionalHeadersGenerator additionalHeadersGenerator = new AdditionalHeadersGenerator(signer);

    @Test
    public void generate() {
        KeyPair keyPair = signer.generateKeys();
        WebHookModel model = new WebHookModel();
        model.setPrivateKey(keyPair.getPrivKey());
        Map<String, String> map = additionalHeadersGenerator.generate(model, "testString");

        String signature = map.get(AdditionalHeadersGenerator.SIGNATURE_HEADER);

        assertNotNull(signature);
    }
}