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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GateTestCases {
	@Test
	public void fetchGateTest() throws IOException {
		final String urlString = "https://api.gateio.ws/api/v4/spot/tickers?currency_pair=HBAR_USDT";
		final String result = "[{\"currency_pair\":\"HBAR_USDT\",\"last\":\"0.31728\",\"lowest_ask\":\"0.31785\"," +
				"\"highest_bid\":\"0.31722\",\"change_percentage\":\"4.7\",\"base_volume\":\"31248953.455663\"," +
				"\"quote_volume\":\"9818031.2911152\",\"high_24h\":\"0.33421\",\"low_24h\":\"0.29551\"}]";
		final InputStream json = new ByteArrayInputStream(result.getBytes());
		final HttpURLConnection connection = mock(HttpURLConnection.class);

		when(connection.getInputStream()).thenReturn(json);

		final CoinFactory factory = new CoinFactory(connection);
		final Gate gate = factory.load(urlString, Gate.class);

		assertNotNull(gate);
		assertEquals(0.31728, gate.getHBarValue());
		assertEquals(31248953.455663, gate.getVolume());
	}
}
