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
 * Represents a Liquid Exchange response.
 *
 * @author Anirudh, Cesar
 */
public class Liquid extends ExchangeCoin {

	@JsonProperty(value="last_traded_price",access = JsonProperty.Access.WRITE_ONLY)
	private Double exchangeRate;

	@JsonProperty(value="product_type",access = JsonProperty.Access.WRITE_ONLY)
	private String productType;

	@JsonProperty(value="code", access = JsonProperty.Access.WRITE_ONLY)
	private String code;

	@JsonProperty(value="volume_24h", access = JsonProperty.Access.WRITE_ONLY)
	private Double volume;

	@Override
	public Double getHBarValue() {
		return this.exchangeRate;
	}

	@Override
	public Double getVolume() {
		return volume == null || volume <= 1.0 ? 0.0 : this.volume;
	}

	public String getProductType() {
		return this.productType;
	}

	public String getCode() {
		return this.code;
	}
}
