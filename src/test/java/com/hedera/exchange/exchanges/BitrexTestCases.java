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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BitrexTestCases {

	@Test
	public void fetchBitrexTest() throws IOException {
		final String urlString = "https://api.bittrex.com/api/v1.1/public/getticker?market=BTC-LTC";
		final String result = "{\"success\":true,\"message\":\"Data Sent\",\"result\":[{\"Bid\":0.00952751,\"Ask\":0.00753996," +
				"\"Volume\":3219496.21980385,\"Last\":0.00954162}]}";
		final InputStream json = new ByteArrayInputStream(result.getBytes());
		final HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getInputStream()).thenReturn(json);

		final CoinFactory factory = new CoinFactory(connection);

		final Bitrex bitrex = factory.load(urlString, Bitrex.class);
		assertTrue(bitrex.isSuccess());
		assertEquals(0.00954162, bitrex.getHBarValue());
		assertEquals("Data Sent", bitrex.getMessage());
		assertEquals(3219496.21980385, bitrex.getVolume());
	}

	@Test
	public void fetchBitrexWithNullResultTest() throws IOException {
		final String result = "{\"success\":false,\"message\":\"INVALID_MARKET\",\"result\": null}";
		final InputStream json = new ByteArrayInputStream(result.getBytes());
		final HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getInputStream()).thenReturn(json);

		final CoinFactory factory = new CoinFactory(connection);

		final Bitrex bitrex = factory.load("https://mock.bittrex.com/getticker?market=BTC-LTC", Bitrex.class);
		assertTrue(bitrex.isSuccess());

		assertFalse(bitrex.isSuccess());
		assertNull(bitrex.getHBarValue());
		assertNull(bitrex.getResult());
	}
}
