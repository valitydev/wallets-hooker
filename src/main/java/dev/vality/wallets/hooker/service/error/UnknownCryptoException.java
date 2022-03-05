package dev.vality.wallets.hooker.service.error;

public class UnknownCryptoException extends RuntimeException {
    public UnknownCryptoException(Throwable e) {
        super(e);
    }
}
