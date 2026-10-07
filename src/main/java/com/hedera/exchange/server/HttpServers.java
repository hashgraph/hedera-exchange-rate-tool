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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.prometheus.metrics.exporter.httpserver.MetricsHandler;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

final class HttpServers {

	private HttpServers() {
		throw new UnsupportedOperationException("Utility class");
	}

	static int port() {
		return Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
	}

	/**
	 * An HTTP server with /metrics (Prometheus format) and /healthz. Callers add their own contexts, then start it.
	 */
	static HttpServer create(final int port) throws IOException {
		JvmMetrics.builder().register();

		final HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		server.setExecutor(Executors.newCachedThreadPool());
		server.createContext("/metrics", new MetricsHandler());
		server.createContext("/healthz", exchange -> send(exchange, 200, "OK", Map.of()));
		return server;
	}

	static void send(final HttpExchange exchange, final int status, final String body,
			final Map<String, String> headers) throws IOException {
		final byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
		headers.forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}
}
