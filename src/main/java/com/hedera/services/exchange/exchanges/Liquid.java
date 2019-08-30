package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Liquid extends AbstractExchange {

	@JsonProperty("exchange_rate")
	private Double exchangeRate;

	@Override
	public Double getHBarValue() {
		return this.exchangeRate;
	}

	public static Liquid load(final String endpoint) {
		return load(endpoint, Liquid.class);
	}
}
