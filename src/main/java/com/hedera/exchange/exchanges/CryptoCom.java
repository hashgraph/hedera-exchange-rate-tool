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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Implements a Crypto.com Exchange Response
 */
public class CryptoCom extends ExchangeCoin {
    @JsonProperty(value="result", access = JsonProperty.Access.WRITE_ONLY)
    private TickerResult result;

    @Override
    public Double getHBarValue() {
        return this.result.data.get(0).askPrice;
    }

    @Override
    public Double getVolume() {
        final  var volume = this.result.data.get(0).volume;
        return volume == null || volume <= 1.0 ? 0.0 : volume;
    }

    private static class TickerData {
        @JsonProperty("a")
        private Double askPrice;

        @JsonProperty("v")
        private Double volume;
    }

    private static class TickerResult {
        @JsonProperty("data")
        private List<TickerData> data;
    }
}
