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
