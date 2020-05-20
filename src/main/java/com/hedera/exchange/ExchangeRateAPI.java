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

import com.hedera.exchange.database.AWSDBParams;
import com.hedera.exchange.database.ExchangeDB;
import com.hedera.exchange.database.ExchangeRateAWSRD;

import java.util.HashMap;
import java.util.Map;

/**
 * This class implements an API that one can trigger using an AWS lambda for example and get the latest Exchange rate file
 * from the database.
 *
 * @author Anirudh, Cesar
 */
public class ExchangeRateAPI {

	private static Map<String, String> HEADERS = new HashMap<>();

	static {
		HEADERS.put("Access-Control-Allow-Origin", "*");
	}

	public static LambdaResponse getLatest() throws Exception {
		final ExchangeDB exchangeDb = new ExchangeRateAWSRD(new AWSDBParams());
		final ExchangeRate latestExchangeRate = exchangeDb.getLatestExchangeRate();
		if (latestExchangeRate == null) {
			return new LambdaResponse(200, "No exchange rate available yet");
		}

		return new LambdaResponse(200, latestExchangeRate.toJson());
	}

	public static class LambdaResponse {
		private int statusCode;

		private String body;

		LambdaResponse(final int statusCode, final String body) {
			this.statusCode = statusCode;
			this.body = body;
		}

		public int getStatusCode() {
			return statusCode;
		}

		public String getBody() {
			return body;
		}

		public boolean isBase64Encoded() {
			return false;
		}

		public Map<String, String> getHeaders() {
			return HEADERS;
		}
	}
}
