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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BitMartTestCases {
	@Test
	public void fetchBitMartTest() throws IOException {
		final String urlString = "https://api-cloud.bitmart.com/contract/v1/tickers?contract_symbol=HBARUSDT";
		final String result = "{\"message\":\"OK\",\"code\":1000,\"trace\":\"2fee4271-b061-4de4-b689-762391485221\"," +
				"\"data\":{\"tickers\":[{\"contract_symbol\":\"HBARUSDT\",\"last_price\":\"0.24737\"," +
				"\"index_price\":\"0.24735492\",\"last_funding_rate\":\"0.00010000\",\"price_change_percent_24h\":\"-6" +
				".210\",\"volume_24h\":\"11275149.882476\",\"url\":\"https://futures.bitmart.com/en?symbol=HBARUSDT\"," +
				"\"high_price\":\"0.26788\",\"low_price\":\"0.24039\",\"legal_coin_price\":\"0.24721699\"}]}}";
		final InputStream json = new ByteArrayInputStream(result.getBytes());
		final HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getInputStream()).thenReturn(json);

		final CoinFactory factory = new CoinFactory(connection);

		final BitMart bitmart = factory.load(urlString, BitMart.class);

		assertNotNull(bitmart);
		assertEquals("OK", bitmart.getMessage());
		assertEquals(0.24737, bitmart.getHBarValue());
		assertEquals(11275149.882476, bitmart.getVolume());
	}
}
