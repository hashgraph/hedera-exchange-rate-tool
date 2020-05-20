package com.hedera.exchange;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

	@ParameterizedTest
	@CsvSource({
			"1581638400,true",
			"1581639400,false",
			"1581724800,true"
	})
	public void isMidnightRate(long expirationTime, boolean isMidnight) {
		Rate currRate = new Rate(30000, 120000, expirationTime);
		Rate nextRate = new Rate(30000, 120000, expirationTime+3600);
		ExchangeRate exchangeRate = new ExchangeRate(currRate, nextRate);

		assertEquals(isMidnight, exchangeRate.isMidnightTime());
	}
}
