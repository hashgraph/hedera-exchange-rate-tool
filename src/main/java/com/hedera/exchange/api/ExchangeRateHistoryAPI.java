package com.hedera.exchange.api;

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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hedera.exchange.ExchangeRate;
import com.hedera.exchange.Rate;
import com.hedera.exchange.database.ExchangeDB;
import com.hedera.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This class implements an API which returns the data from the last 'n'[defaulted to 5] successful runs of ERT
 *
 * @author anighanta
 */
public class ExchangeRateHistoryAPI {

    private static final Logger LOGGER = LogManager.getLogger(ExchangeRateHistoryAPI.class);
    private static final int DEFAULT_NO_OF_RECORDS = 5;
    private final static long BOUND = 25;
    private final static long SECONDS_IN_HOUR = 3_600;
    private final static long SECONDS_IN_DAY = 86_400;
    private final static long HBAR_EQUIV = 30_000;
    private static final DateTimeFormatter UTC_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneOffset.UTC);

    /**
     * @param noOfRecordsParam value of the no_of_records query parameter, or null to use the default
     */
    public ApiResponse getHistory(final ExchangeDB exchangeDb, final String noOfRecordsParam) {
        try{
            final int noOfRecords = noOfRecordsParam != null ?
                    Integer.parseInt(noOfRecordsParam) : DEFAULT_NO_OF_RECORDS;

            LOGGER.info(Exchange.EXCHANGE_FILTER, "params received : {}", noOfRecords);
            ExchangeRate midnightRate = exchangeDb.getLatestMidnightExchangeRate();
            long currMidnightTime = midnightRate.getNextExpirationTimeInSeconds();
            LOGGER.info(Exchange.EXCHANGE_FILTER, "current Midnight time : {}", currMidnightTime);
            LOGGER.info(Exchange.EXCHANGE_FILTER, "current midnight rate : {}", midnightRate.toJson());
            String latestQueriedRate = exchangeDb.getLatestQueriedRate();
            ExchangeRate latestExchangeRate = exchangeDb.getLatestExchangeRate();

            long latestExpirationTime = latestExchangeRate.getNextExpirationTimeInSeconds();

            List<ExchangeRateHistory> results = new ArrayList<>();

            double latestMedian = findMedian(latestQueriedRate);

            results.add(new ExchangeRateHistory(toDate(latestExpirationTime),
                            latestQueriedRate,
                            latestMedian,
                            isSmoothed(midnightRate.getNextRate(), latestMedian),
                            midnightRate,
                            latestExchangeRate.getCurrentRate(),
                            latestExchangeRate.getNextRate()
                    )
            );

            long expirationTime = latestExpirationTime;

            for (int i = 1; i < noOfRecords; i++) {
                expirationTime -= SECONDS_IN_HOUR;
                //pull the appropriate midnight rate
                if (expirationTime <= currMidnightTime) {
                    LOGGER.info(Exchange.EXCHANGE_FILTER, "day changed. fetching older midnight rate");
                    currMidnightTime -= SECONDS_IN_DAY;
                    midnightRate = exchangeDb.getMidnightExchangeRate(currMidnightTime);
                    LOGGER.info(Exchange.EXCHANGE_FILTER, "adjusted current Midnight time : {}", currMidnightTime);
                    LOGGER.info(Exchange.EXCHANGE_FILTER, "adjusted current midnight rate : {}", midnightRate.toJson());
                }

                ExchangeRate currentExchangeRate = exchangeDb.getExchangeRate(expirationTime);
                String currentQueriedRate = exchangeDb.getQueriedRate(expirationTime);
                double currentMedian = findMedian(currentQueriedRate);
                results.add(new ExchangeRateHistory(toDate(expirationTime),
                        currentQueriedRate,
                        calculateMedian(currentExchangeRate),
                        isSmoothed(midnightRate.getNextRate(), currentMedian),
                        midnightRate,
                        currentExchangeRate.getCurrentRate(),
                        currentExchangeRate.getNextRate()
                        )
                );
            }
            String result = "";
            for (ExchangeRateHistory ERH : results) {
                result += ERH.toJson() + ",";
            }

            result = result.replaceAll("\\],\\[", ",");
            result = result.substring(0, result.length() - 1);

            return new ApiResponse(200, result);
        } catch (Exception e){
            LOGGER.error(Exchange.EXCHANGE_FILTER, e.getMessage());
            return new ApiResponse(400, e.getMessage());
        }
    }

    private static double calculateMedian(ExchangeRate exchangeRate){
        LOGGER.info(Exchange.EXCHANGE_FILTER, "calculating median");
        double median = ((double) exchangeRate.getNextRate().getCentEquiv() / exchangeRate.getNextRate().getHBarEquiv()) / 100 ;

        BigDecimal medianBD = new BigDecimal(Double.toString(median));
        medianBD = medianBD.setScale(5, RoundingMode.HALF_UP);
        return medianBD.doubleValue();
    }

    private static String toDate(long expirationTime){
        LOGGER.info(Exchange.EXCHANGE_FILTER, "converting epoc to utc date time format");
        return UTC_DATETIME_FORMAT.format(Instant.ofEpochSecond(expirationTime));
    }

    public boolean isSmoothed(Rate midnightRate, double foundMedian) throws JsonProcessingException {
        if ( foundMedian == 0.0) {
            LOGGER.info(Exchange.EXCHANGE_FILTER, "failed to find median" );
            return false;
        }

        final Rate nextRate = new Rate(HBAR_EQUIV,
                (int) (foundMedian * 100 * HBAR_EQUIV),
                midnightRate.getExpirationTimeInSeconds());
        if (midnightRate.isSmallChange(BOUND, nextRate)) {
            LOGGER.info(Exchange.EXCHANGE_FILTER, "Median in bound : {}", nextRate.toJson());
            return false;
        } else {
            LOGGER.info(Exchange.EXCHANGE_FILTER, "Median out of bound : {}", nextRate.toJson());
            return true;
        }
    }

    private Double findMedian(String queriedRate) {
       final  List<Double> hbarValues = extractHbarValues(queriedRate);
        if (hbarValues.isEmpty()) {
            return 0.0;
        }

        if (hbarValues.size() % 2 == 0) {
            return( hbarValues.get( hbarValues.size() / 2 ) +
                    hbarValues.get( (hbarValues.size() / 2 ) - 1 )
            ) / 2;
        } else {
           return hbarValues.get( (hbarValues.size() - 1) / 2);
        }
    }

    private List<Double> extractHbarValues(String queriedRate){
        List<Double> hbarValues = new ArrayList<Double>();
        JsonParser exchangeParser = new JsonParser();
        JsonElement exchangesElement = exchangeParser.parse(queriedRate);
        if (exchangesElement.isJsonArray()) {
            JsonArray exchanges = exchangesElement.getAsJsonArray();
            for( int i = 0 ; i < exchanges.size(); i++) {
                JsonObject exchange = exchanges.get(i).getAsJsonObject();
                JsonElement hbar = exchange.get("HBAR");
                hbarValues.add(hbar.getAsDouble());
            }

            hbarValues.sort(Comparator.comparingDouble(Double::doubleValue));
            LOGGER.info(Exchange.EXCHANGE_FILTER, "queried rates - {}", hbarValues);
        }

        return hbarValues;
    }

}
