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
 * Represents a Bitrex Exchange response.
 *
 * @author Anirudh, Cesar
 */
public final class Bitrex extends ExchangeCoin {

	@JsonProperty(value="success", access = JsonProperty.Access.WRITE_ONLY)
	private boolean success;

	@JsonProperty(value="message", access = JsonProperty.Access.WRITE_ONLY)
	private String message;

	@JsonProperty(value="result", access = JsonProperty.Access.WRITE_ONLY)
	private Result[] results;

	@Override
	public Double getHBarValue() {
		if (results == null || results[0].last == null) {
			return null;
		}

		return this.results[0].last;
	}

	@Override
	public Double getVolume() {
		if (results == null || results[0].volume == null || results[0].volume <= 1.0) {
			return 0.0;
		}

		return this.results[0].volume;
	}

	boolean isSuccess() {
		return success;
	}

	String getMessage() {
		return message;
	}

	private static class Result {

		@JsonProperty("Last")
		private Double last;

		@JsonProperty("BaseVolume")
		private Double volume;
	}
}
