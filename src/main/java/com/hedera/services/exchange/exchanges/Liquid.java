package com.hedera.services.exchange.exchanges;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class Liquid implements Exchange {

	// TODO update to the exact URL that we need.
	private static final String LIQUID_URL = "https://api.liquid.com/products/5";

	private static final Liquid DEFAULT = new Liquid();

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
			final URL obj = new URL(LIQUID_URL);
			final HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			final Liquid liquid =  OBJECT_MAPPER.readValue(con.getInputStream(), Liquid.class);
			con.disconnect();
			return liquid;
		} catch (final Exception exception) {
			return DEFAULT;
		}
	}
}
