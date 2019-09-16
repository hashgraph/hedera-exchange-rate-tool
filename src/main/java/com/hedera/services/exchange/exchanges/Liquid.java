package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Liquid extends AbstractExchange {

	@JsonProperty(value="exchange_rate",access = JsonProperty.Access.WRITE_ONLY)
	private Double exchangeRate;

	@JsonProperty(value="product_type",access = JsonProperty.Access.WRITE_ONLY)
	private String productType;

	@JsonProperty(value="code", access = JsonProperty.Access.WRITE_ONLY)
	private String code;

	@Override
	@JsonProperty("HBAR")
	public Double getHBarValue() {
		return this.exchangeRate;
	}

	public String getProductType() {
		return this.productType;
	}

	public String getCode() {
		return this.code;
	}

	public static Liquid load(final String endpoint) {
		return load(endpoint, Liquid.class);
	}
}
