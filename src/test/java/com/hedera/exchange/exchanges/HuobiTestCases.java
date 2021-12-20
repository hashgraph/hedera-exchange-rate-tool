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

public class HuobiTestCases {
	@Test
	public void fetchHuobiTest() throws IOException {
		final String urlString = "https://api.huobi.pro/market/detail?symbol=hbarusdt";
		final String result = "{\"ch\":\"market.hbarusdt.detail\",\"status\":\"ok\",\"ts\":1640018488335," +
				"\"tick\":{\"id\":203159482836,\"low\":0.29469,\"high\":0.334499,\"open\":0.316793,\"close\":0.302723," +
				"\"vol\":3370865.073753724,\"amount\":1.0672409331304947E7,\"version\":203159482836,\"count\":29007}}";
		final InputStream json = new ByteArrayInputStream(result.getBytes());
		final HttpURLConnection connection = mock(HttpURLConnection.class);

		when(connection.getInputStream()).thenReturn(json);

		final CoinFactory factory = new CoinFactory(connection);
		final Huobi huobi = factory.load(urlString, Huobi.class);

		assertNotNull(huobi);
		assertEquals("ok", huobi.getStatus());
		assertEquals(0.302723, huobi.getHBarValue());
		assertEquals(3370865.073753724, huobi.getVolume());
	}
}
