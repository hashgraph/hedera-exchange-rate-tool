package com.hedera.services.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Rate {

    private static final int HBARS_IN_CENTS = 100_000;

    @JsonProperty("hbarEquiv")
    private final int hbarEquiv;

    @JsonProperty("centEquiv")
    private final int centEquiv;

    @JsonProperty("expirationTime")
    private final ExpirationTime expirationTime;

    public Rate() {
        this(0.0, 0);
    }

    public Rate(final Double centEquiv, final long expirationTimeInSeconds){
        this.hbarEquiv = HBARS_IN_CENTS;
        this.centEquiv = (int) (this.hbarEquiv * centEquiv);
        this.expirationTime = new ExpirationTime(expirationTimeInSeconds);
    }

    public long getExpirationTimeInSeconds() {
        return this.expirationTime.seconds;
    }

    public int getCentEquiv() {
        return this.centEquiv;
    }

    public int getHBarEquiv() {
        return this.hbarEquiv;
    }

    private static class ExpirationTime {

        @JsonProperty("seconds")
        private long seconds;

        private ExpirationTime(final long seconds) {
            this.seconds = seconds;
        }
    }
}
