package com.hedera.services.exchange.exchanges;

import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BitrexTestCases {

	@Test
	public void fetchBitrexTest() throws IOException {
		final String result = "{\"success\":true,\"message\":\"Data Sent\",\"result\":{\"Bid\":0.00952751,\"Ask\":0.00753996," +
				"\"Last\":0.00954162}}";
		final InputStream json = new ByteArrayInputStream(result.getBytes());
		final HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getInputStream()).thenReturn(json);
		new MockUp<Bitrex>() {
			@Mock
			HttpURLConnection getConnection(final URL url) {
				return connection;
			}
		};

		final Bitrex bitrex = Bitrex.load("https://api.bittrex.com/api/v1.1/public/getticker?market=BTC-LTC");
		assertTrue(bitrex.isSuccess());
		assertEquals(0.00954162, bitrex.getHBarValue());
		assertEquals("Data Sent", bitrex.getMessage());
	}

	@Test
	public void fetchBitrexWithNullResultTest() throws IOException {
		final String result = "{\"success\":false,\"message\":\"INVALID_MARKET\",\"result\": null}";
		final InputStream json = new ByteArrayInputStream(result.getBytes());
		final HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getInputStream()).thenReturn(json);
		new MockUp<Bitrex>() {
			@Mock
			HttpURLConnection getConnection(final URL url) {
				return connection;
			}
		};

		final Bitrex bitrex = Bitrex.load("https://api.bittrex.com/api/v1.1/public/getticker?market=BTC-LTC");
		assertFalse(bitrex.isSuccess());
		assertNull(bitrex.getHBarValue());
		assertNull(bitrex.getResult());
	}
}
