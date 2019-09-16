package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class AbstractExchange implements Exchange {

	private static final Logger LOGGER = LogManager.getLogger(AbstractExchange.class);

	@JsonProperty("Query")
	String endPoint = "";

	public static <T extends Exchange> T load(final String endpoint, final Class<T> type) {
		try {
			final URL url = new URL(endpoint);
			final HttpURLConnection con = getConnection(url);
			if(type.getName() == "com.hedera.services.exchange.exchanges.OkCoin") {
				con.setRequestProperty("X-CoinAPI-Key", OkCoin.APIKEY);
			}
			final T exchange =  OBJECT_MAPPER.readValue(con.getInputStream(), type);
			exchange.setEndPoint(endpoint);
			con.disconnect();
			return exchange;
		} catch (final Exception exception) {
			System.out.println(exception.getMessage());
			return null;
		}
	}

	@Override
	public String toJson() throws JsonProcessingException {
		return OBJECT_MAPPER.writeValueAsString(this);
	}

	public void setEndPoint(String url) {
		endPoint = url;
	}

	private static HttpURLConnection getConnection(final URL url) throws IOException {
		return (HttpURLConnection) url.openConnection();
	}
}
