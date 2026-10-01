package com.hedera.exchange.server;

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
 *
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
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExchangeRateToolServiceTestCases {

	private static final long HOUR = 3_600;
	private static final long OFFSET = 30;
	// 2026-09-30T10:00:00Z
	private static final long TEN_AM = 1_790_762_400L;

	@Test
	void nextRunIsTheNextBoundaryPlusOffset() {
		assertEquals(TEN_AM + OFFSET, ExchangeRateToolService.nextRunAfter(TEN_AM, HOUR, OFFSET));
		assertEquals(TEN_AM + HOUR + OFFSET, ExchangeRateToolService.nextRunAfter(TEN_AM + OFFSET, HOUR, OFFSET));
		assertEquals(TEN_AM + HOUR + OFFSET, ExchangeRateToolService.nextRunAfter(TEN_AM + 1_800, HOUR, OFFSET));
	}

	@Test
	void timerFiringEarlyDoesNotRunTwiceInTheSamePeriod() {
		final long scheduled = TEN_AM + OFFSET;
		final long firedEarly = scheduled - 1;
		assertEquals(scheduled + HOUR, ExchangeRateToolService.runAfter(scheduled, firedEarly, HOUR, OFFSET));
	}

	@Test
	void missedRunsAreSkippedNotReplayed() {
		final long scheduled = TEN_AM + OFFSET;
		final long threeHoursLate = scheduled + 3 * HOUR + 10;
		assertEquals(TEN_AM + 4 * HOUR + OFFSET,
				ExchangeRateToolService.runAfter(scheduled, threeHoursLate, HOUR, OFFSET));
	}

	@Test
	void offsetMustBeWithinTheFrequency() {
		assertThrows(IllegalArgumentException.class, () -> new ExchangeRateToolService(new String[0], HOUR, HOUR));
		assertThrows(IllegalArgumentException.class, () -> new ExchangeRateToolService(new String[0], HOUR, -1));
	}
}
