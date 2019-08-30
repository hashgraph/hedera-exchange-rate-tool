package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Bitrex extends AbstractExchange {

	@JsonProperty("result")
	private Result result;


	@Override
	public Double getHBarValue() {
		if (result == null) {
			return null;
		}
		return this.result.last;
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
