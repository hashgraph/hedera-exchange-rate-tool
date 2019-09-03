package com.hedera.services.exchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ExchangeRateTestCases {

	@Test
	public void fromJson() throws IOException {
		final String json = "{\"CurrentRate\":{\"hbarEquiv\":100000,\"centEquiv\":714," +
				"\"expirationTime\":1567490400000},\"NextRate\":{\"hbarEquiv\":100000,\"centEquiv\":678," +
				"\"expirationTime\":1567490403600}}";

		final ExchangeRate exchangeRate = ExchangeRate.fromJson(json);
	}
}
