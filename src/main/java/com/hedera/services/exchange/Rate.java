package com.hedera.services.exchange;

public class Rate {
    private final Integer hbarEquiv;
    private final Double centEquiv;
    private final Double expirationTime;

    public Rate() {
        this(0, 0.0, 0.0);
    }

    public Rate(Integer hbarEquiv, Double centEquiv, Double expirationTime){
        this.hbarEquiv = hbarEquiv;
        this.centEquiv = centEquiv;
        this.expirationTime = expirationTime;
    }
}
