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

public class Gate extends ExchangeCoin{
	@JsonProperty(value="last", access = JsonProperty.Access.WRITE_ONLY)
	private Double last;

	@JsonProperty(value="base_volume", access = JsonProperty.Access.WRITE_ONLY)
	private Double volume;

	@Override
	public Double getHBarValue() {
		return last;
	}

	@Override
	public Double getVolume() {
		if (volume == null || volume <= 1.0) {
			return 0.0;
		}

		return volume;
	}
}
