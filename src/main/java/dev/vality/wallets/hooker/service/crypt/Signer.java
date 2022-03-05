package dev.vality.wallets.hooker.service.crypt;

public interface Signer {
    String sign(String data, String secret);

    KeyPair generateKeys();
}
