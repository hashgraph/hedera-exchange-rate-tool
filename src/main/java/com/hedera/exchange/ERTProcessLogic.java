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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hedera.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/**
 * This class implements the methods that we perform periodically to generate Exchange rate
 * Operations Performed:
 * <ul>
 *  <li>load Exchanges using URLs provided and fetch HBAR-USD exchange rate.</li>
 *  <li>Calculate the median of the fetched exchange rates.</li>
 *  <li>Check if the median is valid : if small change ..i.e., if in bound.</li>
 *  <li>if not a small change clip it.</li>
 *  <li>check if the clipped rate is more then floor if not floor the rate.</li>
 *  <li>generate ExchangeRate file using the final calculated rate and return it.</li>
 *</ul>
 *  
 * @author Anirudh, Cesar
 */
public class ERTProcessLogic {

    private static final Logger LOGGER = LogManager.getLogger(ERTProcessLogic.class);

    private final long bound;
    private final long floor;
    private List<Exchange> exchanges;
    private final ExchangeRate midnightExchangeRate;
    private Rate currentExchangeRate;
    private final long hbarEquiv;
    private final long frequencyInSeconds;

    public ERTProcessLogic(final long hbarEquiv,
            final List<Exchange> exchanges,
            final long bound,
            final long floor,
            final ExchangeRate midnightExchangeRate,
            final Rate currentExchangeRate,
            final long frequencyInSeconds) {
        this.hbarEquiv = hbarEquiv;
        this.exchanges = exchanges;
        this.bound = bound;
        this.floor = floor;
        this.midnightExchangeRate = midnightExchangeRate;
        this.currentExchangeRate = currentExchangeRate;
        this.frequencyInSeconds = frequencyInSeconds;
    }

    /**
     * Main method that executed the Logic to generate a new Exchange Rate by fetching the rates from respective Exchanges
     * and calculating the median among them. ALso perform isSmallChange checks and clip if necessary and floor the rate if
     * its lower then recommended value.
     * @return ExchangeRate object
     */
    public ExchangeRate call() {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Start of ERT Logic");

        try {
            LOGGER.info(Exchange.EXCHANGE_FILTER, "Generating exchange objects");
            currentExchangeRate.setExpirationTime(ExchangeRateUtils.getCurrentExpirationTime());
            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Setting next hour as current expiration time :{}",
                    currentExchangeRate.getExpirationTimeInSeconds());
            final long nextExpirationTimeInSeconds = currentExchangeRate.getExpirationTimeInSeconds() + frequencyInSeconds;

            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Setting next-next hour as next expiration time :{}",
                    nextExpirationTimeInSeconds);

            final Double medianExRate = calculateMedianRate(exchanges);
            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Median calculated : {}", medianExRate);

            if(medianExRate == null){
                LOGGER.warn(Exchange.EXCHANGE_FILTER, "No median computed. Using current rate as next rate: {}",
                        this.currentExchangeRate.toJson());
                final Rate nextRate = new Rate(this.currentExchangeRate.getHBarEquiv(),
                        this.currentExchangeRate.getCentEquiv() ,
                        nextExpirationTimeInSeconds);
                return new ExchangeRate(this.currentExchangeRate, nextRate);
            }

            Rate nextRate = new Rate(this.hbarEquiv,
                    (int) (medianExRate * 100 * this.hbarEquiv),
                    nextExpirationTimeInSeconds);

            if(midnightExchangeRate != null) {
                if(!midnightExchangeRate.getNextRate().isSmallChange(this.bound, nextRate)) {
                    LOGGER.debug(Exchange.EXCHANGE_FILTER, "last midnight value present. Validating the nextRate with {}",
                            midnightExchangeRate.getNextRate().toJson());
                    nextRate = midnightExchangeRate.getNextRate().clipRate(nextRate, this.bound);
                }
                if(!midnightExchangeRate.getCurrentRate().isSmallChange(this.bound, currentExchangeRate)) {
                    LOGGER.debug(Exchange.EXCHANGE_FILTER, "last midnight value present. Validating the currentRate with {}",
                            midnightExchangeRate.getCurrentRate().toJson());
                    currentExchangeRate = midnightExchangeRate.getCurrentRate().clipRate(currentExchangeRate, this.bound);
                }
            } else {
                LOGGER.debug(Exchange.EXCHANGE_FILTER, "No midnight value found, or the median is not far off " +
                        "Skipping validation of the calculated median");
            }

            LOGGER.debug(Exchange.EXCHANGE_FILTER, "checking floor");
            long newCentEquiv = Math.max(nextRate.getCentEquiv(), floor * nextRate.getHBarEquiv());
            if(newCentEquiv != nextRate.getCentEquiv()){
                LOGGER.warn(Exchange.EXCHANGE_FILTER, "Flooring the rate. calculated : {}, floored to : {}",
                        nextRate.getCentEquiv(), newCentEquiv);
                nextRate = new Rate(nextRate.getHBarEquiv(), newCentEquiv,
                        nextExpirationTimeInSeconds);
            }

            return new ExchangeRate(currentExchangeRate, nextRate);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Return the list of exchanges that worked in json string format using OBJECT_MAPPER
     * @return Json String
     * @throws JsonProcessingException
     */
    public String getExchangeJson() throws JsonProcessingException {
        return Exchange.OBJECT_MAPPER.writeValueAsString(exchanges);
    }

    /**
     * Calculates the Median among the exchange rates fetched from the exchanges
     * @param exchanges - list of Exchange objects that have exchange rates of HABR-USD
     * @return median of the exchange rates
     */
    private Double calculateMedianRate(final List<Exchange> exchanges) throws Exception {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Computing median");

        LOGGER.info(Exchange.EXCHANGE_FILTER, "removing all invalid exchanges retrieved");
        exchanges.removeIf(x -> x == null || x.getHBarValue() == null || x.getHBarValue() == 0.0);

        if(exchanges.size() == 0){
            LOGGER.error(Exchange.EXCHANGE_FILTER, "No valid exchange rates retrieved.");
            return null;
        }

        exchanges.sort(Comparator.comparingDouble(Exchange::getHBarValue));
        LOGGER.info(Exchange.EXCHANGE_FILTER,"exchanges worked {} ", getExchangeJson());
        LOGGER.info(Exchange.EXCHANGE_FILTER, "sorted the exchange rates... calculate the weighted median now");

        double[] exchangeRates = new double[exchanges.size()];
        double[] exchangeVolumes = new double[exchanges.size()];

        int index = 0;
        for(Exchange exchange : exchanges) {
            exchangeRates[index] = exchange.getHBarValue();
            exchangeVolumes[index] = exchange.getVolume();
            index++;
        }

        return findVolumeWeightedMedian(exchangeRates, exchangeVolumes);
    }

    protected Double findVolumeWeightedMedian(double[] exchangeRates, double[] exchangeVolumes) throws Exception {
        if( areRatesAndVolumesValid(exchangeRates, exchangeVolumes) ) {
            return ExchangeRateUtils.findVolumeWeightedMedianAverage(exchangeRates, exchangeVolumes);
        } else {
            return null;
        }
    }

    private boolean areRatesAndVolumesValid(double[] exchangeRates, double[] exchangeVolumes) {
        if(exchangeVolumes.length == 0 ||
                exchangeRates.length != exchangeVolumes.length) {
            LOGGER.error(Exchange.EXCHANGE_FILTER, "Inconsistent rates and their volumes");
            return false;
        }
        return true;
    }
}
