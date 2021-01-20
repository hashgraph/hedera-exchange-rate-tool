package com.hedera.exchange;

/*-
 * ‌
 * Hedera Exchange Rate Tool
 * ​
 * Copyright (C) 2019 - 2020 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 *
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * * Neither the name of JSR-310 nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hedera.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;

import static com.hedera.exchange.exchanges.Exchange.OBJECT_MAPPER;

/**
 * Rate class represents a basic structure of Hedera token price.
 * We represent the rate as
 * - HabrEquiv : A large enough number to represent CentEquiv as a natural number.
 * - CentEquiv : A number when divided by HbarEquiv will give us HBAR-USD exchange rate.
 * - ExpirationTime : top of the hour when this rate expires.
 *
 * @author Anirudh, Cesar
 */
public class Rate {

    private static final Logger LOGGER = LogManager.getLogger(Rate.class);

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

    /**
     * Check if the next rate calculated is within the bound.
     *
     * @param bound : bound specified int he config file
     * @param nextRate :calculated from the median of exchange rates
     * @return true or false weather the next rate is with in the bound.
     */
    public boolean isSmallChange(final long bound, final Rate nextRate){

        if (this.isSmallChange(bound, this.getCentEquiv(), this.getHBarEquiv(), nextRate.getCentEquiv(), nextRate.getHBarEquiv())){
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
    public Rate clipRate(final Rate newRate, long bound) {
        final Rate oldRate = this;
        final BigInteger k100 = BigInteger.valueOf(100);
        final BigInteger k1 = BigInteger.valueOf(1);
        final BigInteger b100 = BigInteger.valueOf(bound).add(k100);
        final BigInteger oC = BigInteger.valueOf(oldRate.centEquiv);
        final BigInteger oH = BigInteger.valueOf(oldRate.hbarEquiv);
        final BigInteger nH = BigInteger.valueOf(newRate.hbarEquiv);
        final BigInteger d = oH.multiply(b100);
        final long newCent = newRate.centEquiv;
        final long high = oC.multiply(nH).multiply(b100).divide(oH.multiply(k100)).longValue();
        final long low = (nH.multiply(oC).multiply(k100).add(d).subtract(k1)).divide(d).longValue();

        //if it's too high, then return the upper bound
        if (newCent > high) {
            LOGGER.debug(Exchange.EXCHANGE_FILTER, "New rate set is HbarEquiv: {}, CentEquiv: {}",
                    newRate.hbarEquiv, high);
            return new Rate(newRate.hbarEquiv, high, newRate.expirationTime);
        }
        //if it's too low, then return the lower bound
        if (newCent < low) {
            LOGGER.debug(Exchange.EXCHANGE_FILTER, "New rate set is HbarEquiv: {}, CentEquiv: {}",
                    newRate.hbarEquiv, low);
            return new Rate(newRate.hbarEquiv, low, newRate.expirationTime);
        }
        //if it's OK, then return the same object that was passed in
        return newRate;
    }

    /**
     * Get the Rate as a Json String
     * @return json String
     * @throws JsonProcessingException
     */
    public String toJson() throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(this);
    }

    public double getRateinUSD() {
        return (double) centEquiv/hbarEquiv;
    }
}
