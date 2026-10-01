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
 */

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;

/**
 * Prometheus metrics for the ERT job. Alert on these from the metrics backend.
 * The reason labels of ert_update_errors_total are Hedera status names, plus ERROR_BUILDING_HEDERA_CLIENT and
 * RETRYABLE_ERROR.
 */
public final class ERTMetrics {

	public static final Counter RUNS = Counter.builder()
			.name("ert_runs_total")
			.help("ERT runs, by result. A failure means the run aborted before updating any network.")
			.labelNames("result")
			.register();

	public static final Counter NETWORK_UPDATES = Counter.builder()
			.name("ert_network_updates_total")
			.help("Exchange rate file updates sent to a Hedera network, by result")
			.labelNames("network", "result")
			.register();

	public static final Gauge LAST_SUCCESSFUL_UPDATE = Gauge.builder()
			.name("ert_last_successful_update_timestamp_seconds")
			.help("Unix time of the last successful exchange rate file update on a network")
			.labelNames("network")
			.register();

	public static final Counter UPDATE_ERRORS = Counter.builder()
			.name("ert_update_errors_total")
			.help("Errors while updating a network. reason is the Hedera status (e.g. INSUFFICIENT_PAYER_BALANCE, "
					+ "EXCHANGE_RATE_CHANGE_LIMIT_EXCEEDED), ERROR_BUILDING_HEDERA_CLIENT, or RETRYABLE_ERROR")
			.labelNames("network", "reason")
			.register();

	public static final Counter MEDIAN_OUT_OF_BOUND = Counter.builder()
			.name("ert_median_out_of_bound_total")
			.help("Runs where the calculated median was out of bound from the midnight rate and had to be clipped")
			.register();

	public static final Counter NO_MEDIAN_COMPUTED = Counter.builder()
			.name("ert_no_median_computed_total")
			.help("Runs where no exchange returned a usable rate, so the current rate was reused")
			.register();

	private ERTMetrics() {
		throw new UnsupportedOperationException("Utility class");
	}
}
