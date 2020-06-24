package com.hedera.exchange.exchanges;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Represents a Coinbase Exchange response.
 *
 * @author Anirudh, Cesar
 */
public class Coinbase extends ExchangeCoin {

    @JsonProperty(value = "data", access = JsonProperty.Access.WRITE_ONLY)
    private Data data;

    @Override
    @JsonProperty("HBAR")
    public Double getHBarValue() {
        if (this.data == null || this.data.rates == null || !this.data.rates.containsKey("HBAR")) {
            return null;
        }

        return Double.valueOf(this.data.rates.get("HBAR"));
    }

    @Override
    @JsonProperty("volume")
    public Double getVolume() {
        return null;
    }

    @JsonIgnore
    String getCurrency() {
        return this.data.currency;
    }

    private static class Data {

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("rates")
        private Map<String, String> rates;
    }
}
