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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class represents all the data that we pull in to run an instance of Exchange Rate tool
 */
public class ExchangeRateHistory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @JsonProperty("ExpirationTime")
    private String expirationTime;

    @JsonProperty("QueriedRate")
    private String queriedRate;

    @JsonProperty("MedianRate")
    private double medianRate;

    @JsonProperty("Smoothed")
    private boolean isSmoothed;

    @JsonProperty("MidnightRate")
    private ExchangeRate midnightRate;

    @JsonProperty("CurrentRate")
    private Rate currentRate;

    @JsonProperty("NextRate")
    private Rate nextRate;

    @JsonCreator
    public ExchangeRateHistory( @JsonProperty("ExpirationTime") final String expirationTime,
                                @JsonProperty("QueriedRate") final String queriedRate,
                                @JsonProperty("MedianRate") final double medianRate,
                                @JsonProperty("Smoothed") final boolean isSmoothed,
                                @JsonProperty("MidnightRate") final ExchangeRate midnightRate,
                                @JsonProperty("CurrentRate") final Rate currentRate,
                                @JsonProperty("NextRate") final Rate nextRate ) {
        this.expirationTime = expirationTime;
        this.queriedRate = queriedRate;
        this.medianRate = medianRate;
        this.isSmoothed = isSmoothed;
        this.midnightRate = midnightRate;
        this.currentRate = currentRate;
        this.nextRate = nextRate;
    }

    /**
     * Converts the ExchangeRateHistory object into a Json String
     * @return Json String
     * @throws JsonProcessingException
     */
    public String toJson() throws JsonProcessingException {
        final ExchangeRateHistory[] rateHistory = new ExchangeRateHistory[] { this };
        return OBJECT_MAPPER.writeValueAsString(rateHistory);
    }

}
