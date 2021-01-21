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
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 *
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * * Neither the name of JSR-310 nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

	@ParameterizedTest
	@CsvSource({
			"120000,0.04",
			"300000,0.10",
			"225000,0.075",
			"3900000,1.30",
			"15462000,5.154"
	})
	public void testUSDconversion(long centEquiv, double usd) {
		Rate rate = new Rate(30000, centEquiv, 1568592000);
		assertEquals(usd, rate.getRateinUSD());
	}
}
