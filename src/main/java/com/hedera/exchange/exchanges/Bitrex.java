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
public final class Bitrex extends AbstractExchange {

	@JsonProperty(value="success", access = JsonProperty.Access.WRITE_ONLY)
	private boolean success;

	@JsonProperty(value="message", access = JsonProperty.Access.WRITE_ONLY)
	private String message;

	@JsonProperty(value="result", access = JsonProperty.Access.WRITE_ONLY)
	private Result result;

	@Override
	@JsonProperty("HBAR")
	public Double getHBarValue() {
		if (result == null) {
			return null;
		}

		return this.result.last;
	}

	@Override
	public Double getVolume() {
		if (result == null || result.volume == null || result.volume <= 1.0) {
			return 0.0;
		}

		return this.result.volume;
	}

	boolean isSuccess() {
		return success;
	}

	String getMessage() {
		return message;
	}

	Result getResult() {
		return result;
	}

	public static Bitrex load(final String endpoint) {
		return load(endpoint, Bitrex.class);
	}

	private static class Result {

		@JsonProperty("Last")
		private Double last;

		@JsonProperty("BaseVolume")
		private Double volume;
	}
}
