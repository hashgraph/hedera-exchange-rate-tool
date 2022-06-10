package com.hedera.exchange.api;

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
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 *
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * * Neither the name of JSR-310 nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.hedera.exchange.ExchangeRate;
import com.hedera.exchange.database.DBParams;
import com.hedera.exchange.database.ExchangeDB;
import com.hedera.exchange.database.QueryHelper;

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
		final ExchangeDB exchangeDb = new QueryHelper(new DBParams());
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
