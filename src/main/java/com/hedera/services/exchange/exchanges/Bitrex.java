package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


public class Bitrex implements Exchange {

	// TODO update to the exact URL that we need
	private static final String BITREX_URL = "https://api.bittrex.com/api/v1.1/public/getticker?market=BTC-LTC";

	private boolean success;

	private String message;

	private Result result;

	@Override
	public Double getHBarValue() {
		if (result == null) {
			return null;
		}

		return this.result.getLast();
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(final boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(final Result result) {
		this.result = result;
	}

	public static Bitrex load() throws IOException {
		final URL obj = new URL(BITREX_URL);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		final int responseCode = con.getResponseCode();


		final Bitrex bitrex =  OBJECT_MAPPER.readValue(con.getInputStream(), Bitrex.class);
		con.disconnect();
		return bitrex;
	}

	public static class Result {

		@JsonProperty("Last")
		private Double last;

		public Double getLast() {
			return last;
		}

		public void setLast(final Double last) {
			this.last = last;
		}
	}
}
