package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Bitrex extends AbstractExchange {

	@JsonProperty("success")
	@JsonIgnore
	private boolean success;

	@JsonProperty("message")
	@JsonIgnore
	private String message;

	@JsonProperty("result")
	@JsonIgnore
	private Result result;

	@Override
	@JsonProperty("HBAR")
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

	public static Bitrex load(final String endpoint) {
		return load(endpoint, Bitrex.class);
	}

	private static class Result {

		@JsonProperty("Last")
		private Double last;
	}
}
