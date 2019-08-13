package com.hedera.services.exchange.exchanges;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class Bitrex implements Exchange {

	private static final String BITREX_URL = "https://api.bittrex.com/api/v1.1/public/getmarkethistory?market=USD-HBAR";

	private boolean success;

	private String message;

	private Result result;

	@Override
	public Double getHBarValue() {
		if (result == null) {
			return null;
		}

		return this.result.Last;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public static Bitrex load() throws IOException {
		URL obj = new URL(BITREX_URL);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		int responseCode = con.getResponseCode();


		final Bitrex bitrex =  OBJECT_MAPPER.readValue(con.getInputStream(), Bitrex.class);
		con.disconnect();
		return bitrex;
	}

	public static class Result {
		private Double Last;

		public Double getLast() {
			return Last;
		}

		public void setLast(Double last) {
			Last = last;
		}
	}
}
