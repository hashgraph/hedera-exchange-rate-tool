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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RateTestCases {

	@Test
	public void clipFloorWithValidNewRateTest() {
		final long expiration = System.currentTimeMillis() / 1_000 + 3600;
		final Rate rate = new Rate(1, 10, expiration);
		final Rate newRate = new Rate(1, 11, expiration);
		final Rate clippedRate = rate.clipRate(newRate, 25);
		assertEquals(11, clippedRate.getCentEquiv());
	}

	@Test
	public void clipFloorWithHigherNewRateTest() {
		final long expiration = System.currentTimeMillis() / 1_000 + 3600;
		final Rate rate = new Rate(1, 10, expiration);
		final Rate newRate = new Rate(1, 20, expiration);
		final Rate clippedRate = rate.clipRate(newRate, 25);
		assertEquals(12, clippedRate.getCentEquiv());
	}

	@Test
	public void clipFloorWithLowerNewRateTest() {
		final long expiration = System.currentTimeMillis() / 1_000 + 3600;
		final Rate rate = new Rate(1, 10, expiration);
		final Rate newRate = new Rate(1, 2, expiration);
		final Rate clippedRate = rate.clipRate(newRate, 25);
		assertEquals(8, clippedRate.getCentEquiv());
	}

	@Test
	public void clipFloorWithFloorUsedTest() {
		final long expiration = System.currentTimeMillis() / 1_000 + 3600;
		final Rate rate = new Rate(1, 5, expiration);
		final Rate newRate = new Rate(1, 1, expiration);
		final Rate clippedRate = rate.clipRate(newRate, 50);
		assertEquals(4, clippedRate.getCentEquiv());
	}

	@Test
	public void clipFloorWithVeryLowFloorUsedTest() {
		final long expiration = System.currentTimeMillis() / 1_000 + 3600;
		final Rate rate = new Rate(1, 5, expiration);
		final Rate newRate = new Rate(1, 1, expiration);
		final Rate clippedRate = rate.clipRate(newRate, 50);
		assertEquals(4, clippedRate.getCentEquiv());
	}

	@Test
	public void isSmallChangeCheck(){
		int bound = 27;
		Rate midnightRate = new Rate(30000, 120000, 1568592000);

		Rate nextRate = new Rate(30000, 96000, 1568592000);
		assertEquals(true, midnightRate.isSmallChange(25, nextRate));

		nextRate = new Rate(30000, 95999, 1568592000);
		assertEquals(false, midnightRate.isSmallChange(25, nextRate));

		nextRate = new Rate(30000, 9999, 1568592000);
		Rate clippedRate = midnightRate.clipRate(nextRate, bound);

		assertEquals(true, midnightRate.isSmallChange(bound, clippedRate));
	}
}
