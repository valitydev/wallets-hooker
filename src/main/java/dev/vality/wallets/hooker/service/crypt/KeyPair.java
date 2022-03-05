package dev.vality.wallets.hooker.service.crypt;

import lombok.Data;

@Data
public class KeyPair {

    private final String privKey;
    private final String publKey;
}
