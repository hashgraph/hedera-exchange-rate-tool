package com.hedera.services.exchange;

public class Rate {
    private String rateName;
    private Integer hbarEquiv;
    private Double centEquiv;
    private Double expirationTime;

    public Rate(){
        rateName = "";
        hbarEquiv = 0;
        centEquiv = 0.0;
        expirationTime = 0.0;
    }

    public Rate(String rateName, Integer hbarEquiv, Double centEquiv, Double expirationTime){
        this.rateName = rateName;
        this.hbarEquiv = hbarEquiv;
        this.centEquiv = centEquiv;
        this.expirationTime = expirationTime;
    }
}
