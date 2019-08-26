package com.hedera.services.exchange;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Rate implements Comparable<Double >{

    private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);

    private static final int HBARS_IN_CENTS = 100_000;

    @JsonProperty("hbarEquiv")
    private final int hbarEquiv;

    @JsonProperty("centEquiv")
    private final int centEquiv;

    @JsonProperty("expirationTime")
    private final ExpirationTime expirationTime;

    public Rate(final int hbarEquiv, final Double centEquiv, final long expirationTimeInSeconds) {
        this(hbarEquiv, (int) (hbarEquiv * centEquiv), expirationTimeInSeconds);
    }

    public Rate(final int hbarEquiv, final int centEquiv, final long expirationTimeInSeconds) {
        this.hbarEquiv = hbarEquiv;
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
        final double currentExchangeRate = this.getHBarValueInDecimal();
        final double nextExchangeRate = nextRate.getHBarValueInDecimal();

        final double difference = Math.abs(currentExchangeRate - nextExchangeRate);
        final double calculatedDelta = (difference / nextExchangeRate) * 100;
        if (calculatedDelta <= maxDelta){
            LOGGER.log(Level.DEBUG, "Median is Valid");
            return true;
        } else {
            LOGGER.log(Level.ERROR, "Median is Invalid. Out of accepted Delta range. Accepted Delta : {},  calculated delta : {}", maxDelta, calculatedDelta);
            return false;
        }
    }

    @Override
    public int compareTo(final Double hbarValue) {
        final Double currentValue = this.getHBarValueInDecimal();
        return currentValue.compareTo(hbarValue);
    }

    @JsonIgnore
    public double getHBarValueInDecimal() {
        return (double)this.centEquiv / this.hbarEquiv;
    }

    @JsonIgnore
    public double getMaxExchangeRate(final double maxDelta) {
        return this.getHBarValueInDecimal() * ( 1 + (maxDelta / 100.0));
    }

    @JsonIgnore
    public double getMinExchangeRate(final double maxDelta) {
        return this.getHBarValueInDecimal() * ( 1 - (maxDelta / 100.0));
    }

    private static class ExpirationTime {

        @JsonProperty("seconds")
        private long seconds;

        private ExpirationTime(final long seconds) {
            this.seconds = seconds;
        }
    }
}
