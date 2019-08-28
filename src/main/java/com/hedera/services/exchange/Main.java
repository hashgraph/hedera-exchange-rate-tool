package com.hedera.services.exchange;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {

	public static void main(String ... args) throws IOException {
		final HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
		server.createContext("/", t -> {
			final String configFile = new String(t.getRequestBody().readAllBytes());
			try {
				ExchangeRateTool.main(configFile);
			} catch (final Exception ex) {
				throw new RuntimeException(ex);
			}
		});

		server.start();
	}
}
