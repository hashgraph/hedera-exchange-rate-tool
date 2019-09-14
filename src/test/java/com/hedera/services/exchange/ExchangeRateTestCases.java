package com.hedera.services.exchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExchangeRateTestCases {

	@Test
	public void fromArrayJson() throws IOException {
		final String json = "[{\"CurrentRate\":{\"hbarEquiv\":1000000,\"centEquiv\":71400," +
				"\"expirationTime\":1567490400000},\"NextRate\":{\"hbarEquiv\":1000000,\"centEquiv\":67800," +
				"\"expirationTime\":1567490403600}}]";

		final ExchangeRate exchangeRate = ExchangeRate.fromJson(json);

		assertEquals(1000000, exchangeRate.getCurrentRate().getHBarEquiv());
		assertEquals(71400, exchangeRate.getCurrentRate().getCentEquiv());
		assertEquals(1567490400000L, exchangeRate.getCurrentRate().getExpirationTimeInSeconds());

		assertEquals(1000000, exchangeRate.getNextRate().getHBarEquiv());
		assertEquals(67800, exchangeRate.getNextRate().getCentEquiv());
		assertEquals(1567490403600L, exchangeRate.getNextRate().getExpirationTimeInSeconds());
	}

	@Test
	public void fromJson() throws IOException {
		final String json = "{\"CurrentRate\":{\"hbarEquiv\":1000000,\"centEquiv\":71400," +
				"\"expirationTime\":1567490400000},\"NextRate\":{\"hbarEquiv\":1000000,\"centEquiv\":67800," +
				"\"expirationTime\":1567490403600}}";

		final ExchangeRate exchangeRate = ExchangeRate.fromJson(json);

		assertEquals(1000000, exchangeRate.getCurrentRate().getHBarEquiv());
		assertEquals(71400, exchangeRate.getCurrentRate().getCentEquiv());
		assertEquals(1567490400000L, exchangeRate.getCurrentRate().getExpirationTimeInSeconds());

		assertEquals(1000000, exchangeRate.getNextRate().getHBarEquiv());
		assertEquals(67800, exchangeRate.getNextRate().getCentEquiv());
		assertEquals(1567490403600L, exchangeRate.getNextRate().getExpirationTimeInSeconds());
	}
}
