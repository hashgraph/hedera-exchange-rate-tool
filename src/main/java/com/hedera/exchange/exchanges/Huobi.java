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

public class Huobi extends ExchangeCoin{

	@JsonProperty(value="status", access = JsonProperty.Access.WRITE_ONLY)
	private String status;

	@JsonProperty(value="tick", access = JsonProperty.Access.WRITE_ONLY)
	private TickerData tickerData;

	@Override
	public Double getHBarValue() {
		return tickerData == null ? null : tickerData.price;
	}

	@Override
	public Double getVolume() {
		return tickerData == null || tickerData.volume == null || tickerData.volume <= 1.0 ?
				0.0 : tickerData.volume;
	}

	public String getStatus() {
		return status;
	}

	private static class TickerData {
		@JsonProperty(value="close")
		private Double price;

		@JsonProperty(value="vol")
		private Double volume;
	}
}
