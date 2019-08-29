package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Liquid extends AbstractExchange {

	@JsonProperty("exchange_rate")
	private Double exchangeRate;

	@JsonProperty("product_type")
	private String productType;

	@JsonProperty("code")
	private String code;

	private String endPoint;

	@Override
	public void setEndPoint(String url) {
		this.endPoint = url;
	}

	@Override
	public String getEndPoint(){
		return this.endPoint;
	}

	@Override
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
