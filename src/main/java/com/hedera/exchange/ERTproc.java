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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hedera.exchange.exchanges.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;


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
public class ERTproc {

    private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);

    private final long bound;
    private final long floor;
    private List<Exchange> exchanges;
    private final Rate midnightExchangeRate;
    private final Rate currentExchangeRate;
    private final long hbarEquiv;
    private final long frequencyInSeconds;

    public ERTproc(final long hbarEquiv,
            final List<Exchange> exchanges,
            final long bound,
            final long floor,
            final Rate midnightExchangeRate,
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
            currentExchangeRate.setExpirationTime(ERTParams.getCurrentExpirationTime());
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

            if(midnightExchangeRate != null && !midnightExchangeRate.isSmallChange(this.bound, nextRate)) {
                LOGGER.debug(Exchange.EXCHANGE_FILTER, "last midnight value present. Validating the median with {}",
                        midnightExchangeRate.toJson());
                    nextRate = midnightExchangeRate.clipRate(nextRate, this.bound);
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
    public Double calculateMedianRate(final List<Exchange> exchanges) throws Exception {
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

    public Double findVolumeWeightedMedian(double[] exchangeRates, double[] exchangeVolumes) throws Exception {
        if( areRatesAndVolumesValid(exchangeRates, exchangeVolumes) ) {
            return findVolumeWeightedMedianAverage(exchangeRates, exchangeVolumes);
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


    /**
     * Return the weighted median of the given values, using the given weights.
     *
     * The algorithm is equivalent to the following. Draw a bar chart, where bar
     * number i has height value[i] and width weight[i]. At the top edge of each
     * bar, draw a dot in the middle. Connect the dots with straight lines. Find
     * the middle of the X axis: the height of the curve above that point is the
     * weighted median.
     *
     * This differs from the algorithm by Edgeworth in 1888. That algorithm simply
     * returns the height of the bar that is above the midpoint. That is fine if
     * there are many data points. But it can be bad if there are very few bars,
     * and they differ greatly in height. The algorithm used here returns a
     * weighted average of that bar's height and it's neighbor's height, which is
     * often a better fit to the intuitive notion of a good "representative value".
     *
     * @param values
     *      the values for which the median should be found. These must be sorted ascending.
     * @param weights
     *      the positive weight for each value, with higher having more influence
     * @return the weighted median
     */
    private double findVolumeWeightedMedianAverage(double[] values, double[] weights) throws Exception {
        int numberOfElements = values.length;
        double weightOfValueJustBelowMiddle;
        double weightOfValueJustAboveMiddle;
        double weightedAverage;
        double totalWeight = 0;
        double currentWeightSum;
        double nextWeightSum;
        double valueJustBelowMiddle;
        double valueJustAboveMiddle;

        for (int i = 0; i < numberOfElements; i++) {
            totalWeight += weights[i];
        }
        final double targetWeight = totalWeight / 2.0;
        currentWeightSum = weights[0] / 2;

        for (int index = 0; index < numberOfElements; index++) {
            nextWeightSum = currentWeightSum + (weights[index] + (index + 1 >= numberOfElements ? 0 : weights[index + 1])) / 2.0;
            if (nextWeightSum > targetWeight) {
                valueJustBelowMiddle = values[index];
                valueJustAboveMiddle = index + 1 >= numberOfElements ? 0 : values[index + 1];
                weightOfValueJustBelowMiddle = nextWeightSum - targetWeight;
                weightOfValueJustAboveMiddle = targetWeight - currentWeightSum;
                weightedAverage = (valueJustBelowMiddle * weightOfValueJustBelowMiddle +
                        valueJustAboveMiddle * weightOfValueJustAboveMiddle) /
                        (weightOfValueJustBelowMiddle + weightOfValueJustAboveMiddle);
                return weightedAverage;
            }
            currentWeightSum = nextWeightSum;
        }

        LOGGER.error(Exchange.EXCHANGE_FILTER, "This should never Happen. Given values are : \n Rates = " +
                Arrays.toString(values) + "\n Volumes = " + Arrays.toString(weights));
        throw new Exception("Couldn't find weighted median with the given values");
    }
}
