package com.hedera.services.exchange;

import com.hedera.services.exchange.database.AWSDBParams;
import com.hedera.services.exchange.database.ExchangeDB;
import com.hedera.services.exchange.database.ExchangeRateAWSRD;

import java.util.HashMap;
import java.util.Map;

public class ExchangeRateApi {

	public static LambdaResponse getLatest() throws Exception {
		final ExchangeDB exchangeDb = new ExchangeRateAWSRD(new AWSDBParams());
		final ExchangeRate latestExchangeRate = exchangeDb.getLatestExchangeRate();
		if (latestExchangeRate == null) {
			return new LambdaResponse(200, "No exchange rate available yet");
		}

		return new LambdaResponse(200, latestExchangeRate.toJson());
	}

	public static class LambdaResponse {
		private int statusCode;

		private String body;

		private LambdaResponse(final int statusCode, final String body) {
			this.statusCode = statusCode;
			this.body = body;
		}

		public int getStatusCode() {
			return statusCode;
		}

		public String getBody() {
			return body;
		}

		public boolean isBase64Encoded() {
			return false;
		}

		public Map<String, String> getHeaders() {
			return new HashMap<>();
		}
	}
}
