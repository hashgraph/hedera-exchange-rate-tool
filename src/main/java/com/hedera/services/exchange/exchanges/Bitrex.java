package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class Bitrex implements Exchange {

	private static final String BITREX_URL = "https://api.bittrex.com/api/v1.1/public/getticker?market=BTC-LTC";

	private static final Bitrex DEFAULT = new Bitrex();

	private static final URL url;

	static {
		try {
			url = new URL(BITREX_URL);
		} catch (MalformedURLException ex) {
			throw new RuntimeException(ex);
		}
	}

	@JsonProperty("success")
	private boolean success;

	@JsonProperty("message")
	private String message;

	@JsonProperty("result")
	private Result result;

	@Override
	public Double getHBarValue() {
		if (result == null) {
			return null;
		}

		return this.result.last;
	}

	boolean isSuccess() {
		return success;
	}

	String getMessage() {
		return message;
	}

	Result getResult() {
		return result;
	}

	public static Bitrex load() {
		try {
			final HttpURLConnection con = getConnection();
			final Bitrex bitrex =  OBJECT_MAPPER.readValue(con.getInputStream(), Bitrex.class);
			con.disconnect();
			return bitrex;
		} catch (final Exception exception) {
			return DEFAULT;
		}
	}

	private static HttpURLConnection getConnection() throws IOException {
		return (HttpURLConnection) url.openConnection();
	}

	private static class Result {

		@JsonProperty("Last")
		private Double last;
	}
}
