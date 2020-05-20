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

/**
 * Represents a OkCoin Exchange response.
 *
 * @author Anirudh, Cesar
 */
public final class OkCoin extends AbstractExchange {

    @JsonProperty(value="product_id", access = JsonProperty.Access.WRITE_ONLY)
    private String productid;

    @JsonProperty(value="instrument_id", access = JsonProperty.Access.WRITE_ONLY)
    private String instrumentid;

    @JsonProperty(value="last", access = JsonProperty.Access.WRITE_ONLY)
    private double rate;

    @Override
    @JsonProperty("HBAR")
    public Double getHBarValue() {
        return rate;
    }

    public String getProductid() {
        return productid;
    }

    public String getInstrumentid() {
        return instrumentid;
    }

    public static OkCoin load(final String endpoint) {
        return load(endpoint, OkCoin.class);
    }
}
