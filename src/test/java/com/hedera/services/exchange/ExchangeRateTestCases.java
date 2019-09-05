package com.hedera.services.exchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExchangeRateTestCases {

	@Test
	public void fromArrayJson() throws IOException {
		final String json = "[{\"CurrentRate\":{\"hbarEquiv\":100000,\"centEquiv\":714," +
				"\"expirationTime\":1567490400000},\"NextRate\":{\"hbarEquiv\":100000,\"centEquiv\":678," +
				"\"expirationTime\":1567490403600}}]";

		final ExchangeRate exchangeRate = ExchangeRate.fromJson(json);

		assertEquals(100000, exchangeRate.getCurrentRate().getHBarEquiv());
		assertEquals(714, exchangeRate.getCurrentRate().getCentEquiv());
		assertEquals(1567490400000L, exchangeRate.getCurrentRate().getExpirationTimeInSeconds());

		assertEquals(100000, exchangeRate.getNextRate().getHBarEquiv());
		assertEquals(678, exchangeRate.getNextRate().getCentEquiv());
		assertEquals(1567490403600L, exchangeRate.getNextRate().getExpirationTimeInSeconds());
	}

	@Test
	public void fromJson() throws IOException {
		final String json = "{\"CurrentRate\":{\"hbarEquiv\":100000,\"centEquiv\":714," +
				"\"expirationTime\":1567490400000},\"NextRate\":{\"hbarEquiv\":100000,\"centEquiv\":678," +
				"\"expirationTime\":1567490403600}}";

		final ExchangeRate exchangeRate = ExchangeRate.fromJson(json);

		assertEquals(100000, exchangeRate.getCurrentRate().getHBarEquiv());
		assertEquals(714, exchangeRate.getCurrentRate().getCentEquiv());
		assertEquals(1567490400000L, exchangeRate.getCurrentRate().getExpirationTimeInSeconds());

		assertEquals(100000, exchangeRate.getNextRate().getHBarEquiv());
		assertEquals(678, exchangeRate.getNextRate().getCentEquiv());
		assertEquals(1567490403600L, exchangeRate.getNextRate().getExpirationTimeInSeconds());
	}
}
