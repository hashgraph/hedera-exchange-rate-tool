package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Bitrex extends AbstractExchange {

	@JsonProperty(value="success", access = JsonProperty.Access.WRITE_ONLY)
	private boolean success;

	@JsonProperty(value="message", access = JsonProperty.Access.WRITE_ONLY)
	private String message;

	@JsonProperty(value="result", access = JsonProperty.Access.WRITE_ONLY)
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
