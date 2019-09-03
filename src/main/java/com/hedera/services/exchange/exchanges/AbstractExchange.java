package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class AbstractExchange implements Exchange {

	private static final Logger LOGGER = LogManager.getLogger(AbstractExchange.class);

	public static <T extends Exchange> T load(final String endpoint, final Class<T> type) {
		try {
			final URL url = new URL(endpoint);
			final HttpURLConnection con = getConnection(url);
			final T exchange =  OBJECT_MAPPER.readValue(con.getInputStream(), type);
			con.disconnect();
			return exchange;
		} catch (final Exception exception) {
			return null;
		}
	}

	@Override
	public String toJson() throws JsonProcessingException {
		return OBJECT_MAPPER.writeValueAsString(this);
	}

	private static HttpURLConnection getConnection(final URL url) throws IOException {
		return (HttpURLConnection) url.openConnection();
	}
}
