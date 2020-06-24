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

import java.util.*;
import java.util.function.Function;

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

    private static final Map<String, Class<? extends AbstractExchange>> EXCHANGES = new HashMap<>();

    static {
        EXCHANGES.put("bitrex", Bitrex.class);
        EXCHANGES.put("liquid", Liquid.class);
        EXCHANGES.put("coinbase", Coinbase.class);
        EXCHANGES.put("upbit", UpBit.class);
        EXCHANGES.put("okcoin", OkCoin.class);
        EXCHANGES.put("binance", Binance.class);
    }

    private final Map<String, String> exchangeApis;
    private final long bound;
    private final long floor;
    private List<Exchange> exchanges;
    private final Rate midnightExchangeRate;
    private final Rate currentExchangeRate;
    private final long hbarEquiv;
    private final long frequencyInSeconds;

    public ERTproc(final long hbarEquiv,
            final Map<String, String> exchangeApis,
            final long bound,
            final long floor,
            final Rate midnightExchangeRate,
            final Rate currentExchangeRate,
            final long frequencyInSeconds) {
        this.hbarEquiv = hbarEquiv;
        this.exchangeApis = exchangeApis;
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
            exchanges = generateExchanges();
            currentExchangeRate.setExpirationTime(ERTParams.getCurrentExpirationTime());
            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Setting next hour as current expiration time :{}",
                    currentExchangeRate.getExpirationTimeInSeconds());
            final long nextExpirationTimeInSeconds = currentExchangeRate.getExpirationTimeInSeconds() + frequencyInSeconds;

            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Setting next-next hour as next expiration time :{}",
                    nextExpirationTimeInSeconds);

            final Double medianExRate = calculateMedianRate(exchanges);
            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Median calculated : {}", medianExRate);

            if (medianExRate == null){
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
     * Loads the list of Exchange objects with HBAR-USD exchange rate using the URL endpoints provided for each
     * Exchange int he config file.
     * @return List of Exchange objects.
     */
    public List<Exchange> generateExchanges() throws IllegalAccessException, InstantiationException {
        List<Exchange> exchanges = new ArrayList<>();

        for (final Map.Entry<String, String> api : this.exchangeApis.entrySet()) {

            //final Function<String, Exchange> exchangeLoader = EXCHANGES.get(api.getKey());
            Class exchangeClass = EXCHANGES.get(api.getKey());
            Exchange exchange = (Exchange) exchangeClass.newInstance();

            if (exchange == null) {
                LOGGER.error(Exchange.EXCHANGE_FILTER,"API {} not found", api.getKey());
                continue;
            }

            final String endpoint = api.getValue();
            final Exchange actualExchange = exchange.load(endpoint); //exchangeLoader.apply(endpoint);
            if (actualExchange == null) {
                LOGGER.error(Exchange.EXCHANGE_FILTER,"API {} not loaded", api.getKey());
                continue;
            }

            exchanges.add(actualExchange);
        }

        return exchanges;
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
    public Double calculateMedianRate(List<Exchange> exchanges) throws Exception {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Computing median");

        LOGGER.info(Exchange.EXCHANGE_FILTER, "removing all invalid exchanges retrieved");
        exchanges.removeIf(x -> x == null || x.getHBarValue() == null || x.getHBarValue() == 0.0);

        if (exchanges.size() == 0){
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

        return findWeightedMedian(exchangeRates, exchangeVolumes);
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
    public double findWeightedMedian(double[] values, double[] weights) throws Exception {
        int n = values.length;
        double w0 = 0, w1 = 0, m = 0, total, sum, next, v0, v1;
        total = 0;
        for (int i = 0; i < n; i++) {
            total += weights[i];
        }
        sum = weights[0] / 2;
        for (int i = 0; i < n; i++) {
            next = sum + (weights[i] + (i + 1 >= n ? 0 : weights[i + 1])) / 2.0;
            //sum is (sum of weights[0...i-1]) + weights[i]/2
            //next is (sum of weights[0...i]) + weights[i+1]/2
            if (next > total / 2.0) {
                //(sum of weights[0...i]) <= (total / 2) < (sum of weights[0...i+1])
                v0 = values[i];
                v1 = i + 1 >= n ? 0 : values[i + 1];
                w0 = next - total / 2.0;
                w1 = total / 2.0 - sum;
                m = (v0 * w0 + v1 * w1) / (w0 + w1);
                //m is the weighted average of v0 and v1, weighted by w0 and w1, respectively
                return m;
            }
            sum = next;
        }

        LOGGER.error(Exchange.EXCHANGE_FILTER, "This should never Happen. Given values are : \n Rates = " +
                Arrays.toString(values) + "\n Volumes = " + Arrays.toString(weights));
        throw new Exception("Couldn't find weighted median with the given values");
    }
}
