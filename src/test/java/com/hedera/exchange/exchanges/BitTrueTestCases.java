package com.hedera.exchange.exchanges;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BitTrueTestCases {
	@Test
	public void fetchBitTureTest() throws IOException {
		final String urlString = "https://openapi.bitrue.com/api/v1/ticker/24hr?symbol=HBARUSDT";
		final String result = "[{\"symbol\":\"HBARUSDT\",\"priceChange\":\"4.9900\",\"priceChangePercent\":null," +
				"\"weightedAvgPrice\":null,\"prevClosePrice\":null,\"lastPrice\":\"0.31750\",\"lastQty\":null," +
				"\"bidPrice\":null,\"askPrice\":null,\"openPrice\":null,\"highPrice\":\"0.33460\",\"lowPrice\":\"0" +
				".28500\",\"volume\":\"21211758.73883500000000000000000000000000\",\"quoteVolume\":\"21211758" +
				".73883500000000000000000000000000\",\"openTime\":0,\"closeTime\":0,\"firstId\":0,\"lastId\":0," +
				"\"count\":21211758.73883500000000000000000000000000}]";
		final InputStream json = new ByteArrayInputStream(result.getBytes());
		final HttpURLConnection connection = mock(HttpURLConnection.class);

		when(connection.getInputStream()).thenReturn(json);

		final CoinFactory factory = new CoinFactory(connection);
		final BitTrue bitTrue = factory.load(urlString, BitTrue.class);

		assertNotNull(bitTrue);
		assertEquals(0.31750, bitTrue.getHBarValue());
		assertEquals(21211758.73883500000000000000000000000000, bitTrue.getVolume());
	}
}
