package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Liquid implements Exchange {

	// TODO update to the exact URL that we need.
	private static final String LIQUID_URL = "https://api.liquid.com/products/5";

	private static final Liquid DEFAULT = new Liquid();

	private static final URL url;

	static {
		try {
			url = new URL(LIQUID_URL);
		} catch (MalformedURLException ex) {
			throw new RuntimeException(ex);
		}
	}

	@JsonProperty("exchange_rate")
	private Double exchangeRate;

	@JsonProperty("product_type")
	private String productType;

	@JsonProperty("code")
	private String code;

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

	public static Liquid load() {
		try {
			final HttpURLConnection con = getConnection();
			final Liquid liquid =  OBJECT_MAPPER.readValue(con.getInputStream(), Liquid.class);
			con.disconnect();
			return liquid;
		} catch (final Exception exception) {
			return DEFAULT;
		}
	}

	private static HttpURLConnection getConnection() throws IOException {
		return (HttpURLConnection) url.openConnection();
	}
}
