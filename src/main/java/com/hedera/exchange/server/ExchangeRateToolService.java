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

import com.hedera.exchange.ERTParams;
import com.hedera.exchange.ExchangeRateTool;
import com.hedera.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Runs the tool every period, ERT_RUN_OFFSET_SECONDS past the boundary so expiry times are for the new hour. One replica only. */
public class ExchangeRateToolService {

	private static final Logger LOGGER = LogManager.getLogger(ExchangeRateToolService.class);

	private final String[] args;
	private final long frequencySeconds;
	private final long offsetSeconds;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	ExchangeRateToolService(final String[] args, final long frequencySeconds, final long offsetSeconds) {
		if (offsetSeconds < 0 || offsetSeconds >= frequencySeconds) {
			throw new IllegalArgumentException(String.format(
					"ERT_RUN_OFFSET_SECONDS must be in [0, %d), got %d", frequencySeconds, offsetSeconds));
		}
		this.args = args;
		this.frequencySeconds = frequencySeconds;
		this.offsetSeconds = offsetSeconds;
	}

	public static void main(final String... args) throws IOException, InterruptedException {
		// Read the config once up front so a bad config fails the pod at startup instead of at the first run.
		final long frequencySeconds = ERTParams.readConfig(args).getFrequencyInSeconds();
		final long offsetSeconds = Long.parseLong(System.getenv().getOrDefault("ERT_RUN_OFFSET_SECONDS", "30"));
		final ExchangeRateToolService service = new ExchangeRateToolService(args, frequencySeconds, offsetSeconds);

		final int port = HttpServers.port();
		HttpServers.create(port).start();
		LOGGER.info(Exchange.EXCHANGE_FILTER, "Metrics on port {}; running ERT every {}s at offset {}s",
				port, frequencySeconds, offsetSeconds);

		// Wait for the database before the first run, so a pod that starts before it isn't counted as a failed run.
		DatabaseConnection.connectWithRetry();
		service.schedule(nextRunAfter(System.currentTimeMillis() / 1000, frequencySeconds, offsetSeconds));
	}

	static long nextRunAfter(final long nowEpochSeconds, final long frequencySeconds, final long offsetSeconds) {
		return Math.floorDiv(nowEpochSeconds - offsetSeconds, frequencySeconds) * frequencySeconds
				+ offsetSeconds + frequencySeconds;
	}

	/** Derived from the previous run, not the clock, so an early timer can't double-run; missed runs are skipped. */
	static long runAfter(final long previousRun, final long nowEpochSeconds, final long frequencySeconds,
			final long offsetSeconds) {
		final long next = previousRun + frequencySeconds;
		return next > nowEpochSeconds ? next : nextRunAfter(nowEpochSeconds, frequencySeconds, offsetSeconds);
	}

	private void schedule(final long runAtEpochSeconds) {
		final long delayMillis = Math.max(0, runAtEpochSeconds * 1000 - System.currentTimeMillis());
		LOGGER.info(Exchange.EXCHANGE_FILTER, "Next ERT run in {}s", delayMillis / 1000);
		scheduler.schedule(() -> runAndReschedule(runAtEpochSeconds), delayMillis, TimeUnit.MILLISECONDS);
	}

	private void runAndReschedule(final long runAtEpochSeconds) {
		try {
			new ExchangeRateTool().run(args);
		} finally {
			schedule(runAfter(runAtEpochSeconds, System.currentTimeMillis() / 1000, frequencySeconds, offsetSeconds));
		}
	}
}
