package com.hedera.services.exchange;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Rate {

    private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);

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

    public Rate(final Double centEquiv, final long expirationTimeInSeconds) {
        this((int) (HBARS_IN_CENTS * centEquiv), expirationTimeInSeconds);
    }

    public Rate(final int centEquiv, final long expirationTimeInSeconds) {
        this.hbarEquiv = HBARS_IN_CENTS;
        this.centEquiv = centEquiv;
        this.expirationTime = new ExpirationTime(expirationTimeInSeconds);
    }

    @JsonIgnore
    public long getExpirationTimeInSeconds() {
        return this.expirationTime.seconds;
    }

    public int getCentEquiv() {
        return this.centEquiv;
    }

    public int getHBarEquiv() {
        return this.hbarEquiv;
    }

    public boolean isValid(final double maxDelta, final Rate nextRate){
        final long erNowNumTinyCents = this.getCentEquiv();
        final long erNewNumTinyCents = nextRate.getCentEquiv();

        final long difference = Math.abs(erNewNumTinyCents - erNowNumTinyCents);
        final double calculatedDelta = ( (double)difference / erNowNumTinyCents ) * 100;
        if (calculatedDelta <= maxDelta){
            LOGGER.log(Level.DEBUG, "Median is Valid");
            return true;
        }
        else{
            LOGGER.log(Level.ERROR, "Median is Invalid. Out of accepted Delta range. Accepted Delta : {},  calculated delta : {}", maxDelta, calculatedDelta);
            return false;
        }
    }

    private static class ExpirationTime {

        @JsonProperty("seconds")
        private long seconds;

        private ExpirationTime(final long seconds) {
            this.seconds = seconds;
        }
    }
}
