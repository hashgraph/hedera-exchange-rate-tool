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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PayBitoTestCases {
	@Test
	public void retrievePayBitoDataTest() throws Exception {
		final String urlString = "https://stream.paybito.com:8443/SocketStream/api/get24hTicker?counter=HBAR&base=USD";
		final String result = "[{\"lastBidQty\":null,\"lastBidPrice\":null,\"lastAskQty\":null,\"lastAskPrice\":null," +
				"\"yearHighPrice\":null,\"yearLowPrice\":null,\"highLowFlag\":null,\"buySellOfferFlag\":null," +
				"\"buySellOfferAction\":null,\"openPrice\":\"0.32405273\",\"baseCurrency\":\"USD\"," +
				"\"lastTradeTime\":null,\"ltp\":\"0.3155486\",\"closePrice\":\"0.3155486\",\"highPrice\":\"0" +
				".33185076\",\"lowPrice\":\"0.3139456\",\"volume\":\"2723791.57\",\"roc\":\"-2.6950301792\",\"ctp\":\"0" +
				".3155486\",\"currency\":\"HBAR\",\"action\":\"buy\"}]";

		final InputStream json = new ByteArrayInputStream(result.getBytes());
		final HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getInputStream()).thenReturn(json);

		final CoinFactory factory = new CoinFactory(connection);
		final PayBito payBito = factory.load(urlString, PayBito.class);

		assertNotNull(payBito);
		assertEquals(0.3155486, payBito.getHBarValue());
		assertEquals(2723791.57, payBito.getVolume());
	}
}
