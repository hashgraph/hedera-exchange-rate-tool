package com.hedera.services.exchange;

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
		assertEquals(3, clippedRate.getCentEquiv());
	}

	@Test
	public void clipFloorWithVeryLowFloorUsedTest() {
		final long expiration = System.currentTimeMillis() / 1_000 + 3600;
		final Rate rate = new Rate(1, 5, expiration);
		final Rate newRate = new Rate(1, 1, expiration);
		final Rate clippedRate = rate.clipRate(newRate, 50);
		assertEquals(3, clippedRate.getCentEquiv());
	}
}
