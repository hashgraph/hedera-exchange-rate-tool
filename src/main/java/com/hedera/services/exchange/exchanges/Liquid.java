package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Liquid extends AbstractExchange {

	@JsonProperty("exchange_rate")
	@JsonIgnore
	private Double exchangeRate;

	@JsonProperty("product_type")
	@JsonIgnore
	private String productType;

	@JsonProperty("code")
	@JsonIgnore
	private String code;

	@JsonProperty("Query")
	String endPoint = "";

	@Override
	public void setEndPoint(String url) {
		endPoint = url;
	}

	@Override
	@JsonProperty("HBAR")
	public Double getHBarValue() {
		return this.exchangeRate;
	}

	String getProductType() {
		return this.productType;
	}

	String getCode() {
		return this.code;
	}

	public static Liquid load(final String endpoint) {
		return load(endpoint, Liquid.class);
	}
}
