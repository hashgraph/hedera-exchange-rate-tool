package com.hedera.services.exchange;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;

import static com.hedera.services.exchange.exchanges.Exchange.OBJECT_MAPPER;

public class Rate {

    private static final Logger LOGGER = LogManager.getLogger(Rate.class);

    private static long TINY_BARS_IN_HBAR = 1_000_000_000;
    private static long TINY_CENTS_IN_USD = 100_000_000;

    @JsonProperty("hbarEquiv")
    private final long hbarEquiv;

    @JsonProperty("centEquiv")
    private final long centEquiv;

    @JsonProperty("expirationTime")
    private long expirationTime;

    public Rate(final int hbarEquiv, final Double centEquiv, final long expirationTimeInSeconds) {
        this(hbarEquiv, (int) (hbarEquiv * centEquiv), expirationTimeInSeconds);
    }

    @JsonCreator
    public Rate(@JsonProperty("hbarEquiv") final long hbarEquiv, @JsonProperty("centEquiv") final long centEquiv, @JsonProperty("expirationTime") final long expirationTimeInSeconds) {
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

    public long getCentEquiv() {
        return this.centEquiv;
    }

    public long getHBarEquiv() {
        return this.hbarEquiv;
    }

    public boolean isSmallChange(final long bound, final Rate nextRate){

        final long oldExchangeRateInCents = toTinyCents(this.getCentEquiv());
        final long oldExchangeRateInHBars = toTinyBars(this.getHBarEquiv());

        final long newExchangeRateInCents = toTinyCents(nextRate.getCentEquiv());
        final long newExchangeRateInHbars = toTinyBars(nextRate.getHBarEquiv());

        if (this.isSmallChange(bound, oldExchangeRateInCents, oldExchangeRateInHBars, newExchangeRateInCents, newExchangeRateInHbars)){
            LOGGER.debug("Median ({}, {}) is Valid", nextRate.getHBarEquiv(), nextRate.getCentEquiv());
            return true;
        } else {
            LOGGER.error("Median ({},{}) is Invalid.", nextRate.getHBarEquiv(), nextRate.getCentEquiv());
            return false;
        }
    }

    /**
     * Is the new exchange rate valid? The exchange rate of newC tiny cents per newH tinybars is valid
     * if it increases by no more than bound percent, nor decreases by more than the inverse amount.
     *
     * It is defined to be valid iff (for infinite-precision real numbers):
     * <pre>
     *    oldC/oldH * (1 + bound/100)
     * >= newC/newH
     * >= oldC/oldH * 1/(1 + bound/100)
     * </pre>
     *
     * Equivalently, it is valid iff both of the following are true:
     * <pre>
     * oldC * newH * (100 + bound) - newC * oldH * 100 >= 0
     * oldH * newC * (100 + bound) - newH * oldC * 100 >= 0
     * </pre>
     *
     * The expression above is for infinite-precision real numbers. This method actually performs the
     * computations in a way that completely avoids overflow and roundoff errors.
     *
     * All parameters much be positive. There are 100 million tinybars in an hbar, and 100 million
     * tinycents in a USD cent.
     *
     * @param bound max increase is by a factor of (1+bound/100), decrease by 1 over that
     * @param oldC the old exchange rate is for this many tinycents
     * @param oldH the old exchange rate is for this many tinybars
     * @param newC the new exchange rate is for this many tinycents
     * @param newH the new exchange rate is for this many tinybars
     */
    private boolean isSmallChange(long bound, long oldC, long oldH, long newC, long newH) {
        BigInteger k100 = BigInteger.valueOf(100);
        BigInteger b100 = BigInteger.valueOf(bound).add(k100);
        BigInteger oC = BigInteger.valueOf(oldC);
        BigInteger oH = BigInteger.valueOf(oldH);
        BigInteger nC = BigInteger.valueOf(newC);
        BigInteger nH = BigInteger.valueOf(newH);
        return
                bound > 0 && oldC > 0 && oldH > 0 && newC > 0 && newH > 0
                        && oC.multiply(nH).multiply(b100).subtract(
                        nC.multiply(oH).multiply(k100)
                ).signum() >= 0
                        && oH.multiply(nC).multiply(b100).subtract(
                        nH.multiply(oC).multiply(k100)
                ).signum() >= 0;
    }

    @JsonIgnore
    public double getHBarValueInDecimal() {
        return (double)this.centEquiv / (this.hbarEquiv * 100);
    }

    /**
     * Return the exchange rate newRate, clipped to lie within the bound of oldRate. If newRate is already within the
     * bound, then the object is returned unchanged. Otherwise, a new Rate object is instantiated and returned that lies
     * within the bound. The new Rate will have the same hbarEquiv as newRate, but its centEquiv will be closer to the
     * one in oldRate.
     *
     * In other words, newC is adjusted if needed, so that these are both satisfied:
     *
     * newC  <= oldC * newH * (100 + bound) / (100 * oldH)
     * newC  > newH * oldC * 100 / (oldH * (100 + bound))
     *
     * With infinite precision division, the second inequality could be >= but because integer division rounds down,
     * it needs to be a strict > to avoid errors. This means that if newC is clipped to that lower value,
     * and if the division is exact, then it will end up being one greater than it could have been. (To use the
     * more exact bound would be more complicated, and doesn't really make a practical difference).
     *
     * Note that this will return unpredictable results if the bounds would exceed the range of an int. Imagine creating
     * a new Rate with the same hbarEquiv as newRate, but with its centEquiv set to the centEquiv of the oldRate
     * increased or decreased by the maximum allowed by the bound. In that case, if the centEquiv overflows an int, then
     * the result returned by this method will be incorrect. That is unlikely to happen in practice, unless
     * hbarEquiv is too close to 0 or to Integer.MAX_VALUE.  So an hbarEquiv in the middle of the range is best. A
     * choice of 30,000 works well if hbarEquiv is an int.
     *
     * @param newRate
     * 		a proposed new rate
     * @param bound
     * 		the max allowed percentage increase in the rate
     * @return if the new rate is legal, return newRate, otherwise instantiate and return a barely legal rate.
     */
    public Rate clipRate(final Rate newRate, long bound, long floor) {
        final Rate oldRate = this;
        BigInteger k100 = BigInteger.valueOf(100);
        BigInteger b100 = BigInteger.valueOf(bound).add(k100);
        BigInteger oC = BigInteger.valueOf(oldRate.centEquiv);
        BigInteger oH = BigInteger.valueOf(oldRate.hbarEquiv);
        BigInteger nH = BigInteger.valueOf(newRate.hbarEquiv);
        long newCent = newRate.centEquiv;
        long high = oC.multiply(nH).multiply(b100).divide(oH.multiply(k100)).longValue();
        long low = nH.multiply(oC).multiply(k100).divide(oH.multiply(b100)).longValue();

        //if it's too high, then return the upper bound
        if (newCent > high) {
            return new Rate(newRate.hbarEquiv, high, newRate.expirationTime);
        }
        //if it's too low, then return the lower bound
        else if (newCent < low && newCent > (floor * oldRate.hbarEquiv)  ) {
            return new Rate(newRate.hbarEquiv, low, newRate.expirationTime);
        }
        else if( newCent <= (floor * oldRate.hbarEquiv)){
            return new Rate(newRate.hbarEquiv, (floor * oldRate.hbarEquiv), newRate.expirationTime);
        }
        //if it's OK, then return the same object that was passed in
        return newRate;
    }

    public String toJson() throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(this);
    }

    private long toTinyCents(final long rate){
        return rate * 100 * TINY_CENTS_IN_USD;
    }

    private long toTinyBars(final long rate) {
        return rate * TINY_BARS_IN_HBAR;
    }
}
