package com.hedera.services.exchange;

public class Rate {
    private final String rateName;
    private final Integer hbarEquiv;
    private final Double centEquiv;
    private final Double expirationTime;

    public Rate() {
        this("", 0, 0.0, 0.0);
    }

    public Rate(String rateName, Integer hbarEquiv, Double centEquiv, Double expirationTime){
        this.rateName = rateName;
        this.hbarEquiv = hbarEquiv;
        this.centEquiv = centEquiv;
        this.expirationTime = expirationTime;
    }
}
