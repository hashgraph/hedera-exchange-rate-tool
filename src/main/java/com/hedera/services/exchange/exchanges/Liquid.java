package com.hedera.services.exchange.exchanges;

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

	private Double exchange_rate;

	@Override
	public Double getHBarValue() {
		if(exchange_rate == null) {
			return null;
		}

		return this.exchange_rate;
	}

	public Double getExchange_rate() {
		return exchange_rate;
	}

	public void setExchange_rate(Double exchange_rate) {
		this.exchange_rate = exchange_rate;
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
