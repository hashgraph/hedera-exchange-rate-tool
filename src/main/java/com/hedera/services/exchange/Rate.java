package com.hedera.services.exchange;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;
import java.util.TimeZone;

import static com.hedera.services.exchange.exchanges.Exchange.OBJECT_MAPPER;

public class Rate implements Comparable<Double >{

    private static final Logger LOGGER = LogManager.getLogger(Rate.class);

    @JsonProperty("hbarEquiv")
    private final int hbarEquiv;

    @JsonProperty("centEquiv")
    private final int centEquiv;

    @JsonProperty("expirationTime")
    private long expirationTime;

    public Rate(final int hbarEquiv, final Double centEquiv, final long expirationTimeInSeconds) {
        this(hbarEquiv, (int) (hbarEquiv * centEquiv), expirationTimeInSeconds);
    }

    @JsonCreator
    public Rate(@JsonProperty("hbarEquiv") final int hbarEquiv, @JsonProperty("centEquiv") final int centEquiv, @JsonProperty("expirationTime") final long expirationTimeInSeconds) {
        this.hbarEquiv = hbarEquiv;
        this.centEquiv = centEquiv;
        this.expirationTime =  expirationTimeInSeconds;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
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

        final double currentExchangeRate = toTinyCents(this.getHBarValueInDecimal());
        final double nextExchangeRate = toTinyCents(nextRate.getHBarValueInDecimal());

        final double difference = Math.abs(currentExchangeRate - nextExchangeRate);
        final double calculatedDelta = (difference / nextExchangeRate) * 100;
        if (calculatedDelta <= maxDelta){
            LOGGER.debug("Median is Valid");
            return true;
        } else {
            LOGGER.error("Median is Invalid. Out of accepted Delta range. Accepted Delta : {},  calculated delta : {}", maxDelta, calculatedDelta);
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
        return (double)this.centEquiv / (this.hbarEquiv * 100);
    }

    @JsonIgnore
    public double getMaxExchangeRate(final double maxDelta) {
        return this.getHBarValueInDecimal() * ( 1 + (maxDelta / 100.0));
    }

    @JsonIgnore
    public double getMinExchangeRate(final double maxDelta) {
        return this.getHBarValueInDecimal() * ( 1 - (maxDelta / 100.0));
    }

    public String toJson() throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(this);
    }

    @JsonIgnore
    public static long toTinyCents(final double rate){
        long numTinyBars = 1_000_000_000;
        return (long)(rate * 100 * numTinyBars);
    }
}
