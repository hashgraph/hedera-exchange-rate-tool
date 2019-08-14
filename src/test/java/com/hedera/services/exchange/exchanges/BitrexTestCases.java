package com.hedera.services.exchange.exchanges;

import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BitrexTestCases {

	@Test
	public void testRetrieveBitrexData() throws IOException {
		final String result = "{\"success\":true,\"message\":\"Data Sent\",\"result\":{\"Bid\":0.00952751,\"Ask\":0.00753996," +
				"\"Last\":0.00954162}}";
		final InputStream json = new ByteArrayInputStream(result.getBytes());
		final HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getInputStream()).thenReturn(json);
		new MockUp<Bitrex>() {
			@Mock
			HttpURLConnection getConnection() {
				return connection;
			}
		};

		final Bitrex bitrex = Bitrex.load();
		assertTrue(bitrex.isSuccess());
		assertEquals(0.00954162, bitrex.getHBarValue());
		assertEquals("Data Sent", bitrex.getMessage());
	}
}
