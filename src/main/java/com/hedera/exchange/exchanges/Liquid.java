package com.hedera.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Liquid Exchange response.
 *
 * @author Anirudh, Cesar
 */
public class Liquid extends AbstractExchange {

	@JsonProperty(value="last_traded_price",access = JsonProperty.Access.WRITE_ONLY)
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
