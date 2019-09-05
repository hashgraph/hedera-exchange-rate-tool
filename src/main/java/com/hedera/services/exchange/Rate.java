package com.hedera.services.exchange;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;
import java.util.TimeZone;

public class Rate implements Comparable<Double >{

    private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);

    @JsonProperty("hbarEquiv")
    private final int hbarEquiv;

    @JsonProperty("centEquiv")
    private final int centEquiv;

    @JsonProperty("expirationTime")
    private final long expirationTime;

    public Rate(){
        hbarEquiv = 1;
        centEquiv = 12;
        expirationTime = getCurrentExpirationTime();
    }

    public Rate(final int hbarEquiv, final Double centEquiv, final long expirationTimeInSeconds) {
        this(hbarEquiv, (int) (hbarEquiv * centEquiv), expirationTimeInSeconds);
    }

    @JsonCreator
    public Rate(@JsonProperty("hbarEquiv") final int hbarEquiv, @JsonProperty("centEquiv") final int centEquiv, @JsonProperty("expirationTime") final long expirationTimeInSeconds) {
        this.hbarEquiv = hbarEquiv;
        this.centEquiv = centEquiv;
        this.expirationTime =  expirationTimeInSeconds;
    }

    @JsonIgnore
    public long getExpirationTimeInSeconds() {
        return this.expirationTime;
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

    private long getMidnightUTCTime(){
        Calendar currentTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        currentTime.set(currentTime.YEAR, currentTime.MONTH, currentTime.DAY_OF_MONTH, 0,0,0);

        return  currentTime.getTimeInMillis() / 1000;
    }

    private long getCurrentExpirationTime() {
        long currentTime = System.currentTimeMillis();
        long nextHour = ( currentTime - (currentTime % 3600000) ) + 3600000;
        return nextHour;
    }
}
