package com.hedera.exchange.exchanges;

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
