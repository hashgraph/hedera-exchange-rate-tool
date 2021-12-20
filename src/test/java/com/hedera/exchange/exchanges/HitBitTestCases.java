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

public class HitBitTestCases {
	@Test
	public void fetchHitBitTest() throws IOException {
		final String urlString = "https://api.hitbtc.com/api/3/public/ticker/HBARUSDT";
		final String result = "{\"ask\":\"0.316485\",\"bid\":\"0.316247\",\"last\":\"0.316468\",\"low\":\"0.295210\"," +
				"\"high\":\"0.334123\",\"open\":\"0.303178\",\"volume\":\"9375073\",\"volume_quote\":\"2964055" +
				".628989\",\"timestamp\":\"2021-12-20T07:51:36.460Z\"}";
		final InputStream json = new ByteArrayInputStream(result.getBytes());
		final HttpURLConnection connection = mock(HttpURLConnection.class);

		when(connection.getInputStream()).thenReturn(json);

		final CoinFactory factory = new CoinFactory(connection);
		final HitBit hitBit = factory.load(urlString, HitBit.class);

		assertNotNull(hitBit);
		assertEquals(0.316468, hitBit.getHBarValue());
		assertEquals(9375073, hitBit.getVolume());
	}
}
