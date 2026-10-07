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

import com.hedera.exchange.api.ApiResponse;
import com.hedera.exchange.api.ExchangeRateAPI;
import com.hedera.exchange.api.ExchangeRateHistoryAPI;
import com.hedera.exchange.database.ExchangeDB;
import com.hedera.exchange.exchanges.Exchange;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Histogram;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Serves GET /latest (also as /pricing) and GET /history?no_of_records=N. */
public class ExchangeRateApiServer {

	private static final Logger LOGGER = LogManager.getLogger(ExchangeRateApiServer.class);

	private static final Counter REQUESTS = Counter.builder()
			.name("http_requests_total")
			.help("HTTP requests to the exchange rate API")
			.labelNames("path", "status")
			.register();

	private static final Histogram REQUEST_DURATION = Histogram.builder()
			.name("http_request_duration_seconds")
			.help("HTTP request latency of the exchange rate API")
			.labelNames("path")
			.register();

	private interface Handler {
		ApiResponse handle(Map<String, String> queryParams) throws Exception;
	}

	public static void main(final String... args) throws IOException, InterruptedException {
		final ExchangeDB exchangeDb = DatabaseConnection.connectWithRetry();
		final ExchangeRateHistoryAPI historyApi = new ExchangeRateHistoryAPI();

		final int port = HttpServers.port();
		final HttpServer server = HttpServers.create(port);
		route(server, "/latest", params -> ExchangeRateAPI.getLatest(exchangeDb));
		route(server, "/pricing", params -> ExchangeRateAPI.getLatest(exchangeDb));
		route(server, "/history", params -> historyApi.getHistory(exchangeDb, params.get("no_of_records")));
		server.start();
		LOGGER.info(Exchange.EXCHANGE_FILTER, "Exchange rate API listening on port {}", port);
	}

	private static void route(final HttpServer server, final String path, final Handler handler) {
		server.createContext(path, exchange -> {
			final long start = System.nanoTime();
			int status = 500;
			try {
				if (!exchange.getRequestURI().getPath().equals(path)) {
					status = 404;
					HttpServers.send(exchange, status, "Not found", Map.of());
				} else if (!"GET".equals(exchange.getRequestMethod())) {
					status = 405;
					HttpServers.send(exchange, status, "Method not allowed", Map.of("Allow", "GET"));
				} else {
					final ApiResponse response = handler.handle(parseQuery(exchange));
					status = response.getStatusCode();
					final Map<String, String> headers = new HashMap<>(response.getHeaders());
					headers.put("Content-Type", "application/json");
					HttpServers.send(exchange, status, response.getBody(), headers);
				}
			} catch (Exception e) {
				LOGGER.error(Exchange.EXCHANGE_FILTER, "Request to {} failed", path, e);
				status = 500;
				try {
					HttpServers.send(exchange, status, "Internal server error", Map.of());
				} catch (IOException alreadyCommitted) {
					// the response was partially written before the failure; nothing more we can send
				}
			} finally {
				exchange.close();
				REQUESTS.labelValues(path, Integer.toString(status)).inc();
				REQUEST_DURATION.labelValues(path).observe((System.nanoTime() - start) / 1e9);
			}
		});
	}

	private static Map<String, String> parseQuery(final HttpExchange exchange) {
		final Map<String, String> params = new HashMap<>();
		final String rawQuery = exchange.getRequestURI().getRawQuery();
		if (rawQuery == null || rawQuery.isEmpty()) {
			return params;
		}
		for (final String pair : rawQuery.split("&")) {
			final int eq = pair.indexOf('=');
			final String key = eq >= 0 ? pair.substring(0, eq) : pair;
			final String value = eq >= 0 ? pair.substring(eq + 1) : "";
			params.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
		}
		return params;
	}
}
