package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hedera.services.exchange.ERTproc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Bitrex extends AbstractExchange {

	private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);

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

	public static Bitrex load(final String endpoint) {
		LOGGER.debug("Loading exchange rate from Bitrex");
		return load(endpoint, Bitrex.class);
	}

	private static class Result {

		@JsonProperty("Last")
		private Double last;
	}
}
