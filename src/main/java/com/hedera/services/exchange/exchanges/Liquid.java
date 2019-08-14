package com.hedera.services.exchange.exchanges;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class Liquid implements Exchange {

	// TODO update to the exact URL that we need.
	private static final String LIQUID_URL = "https://api.liquid.com/products/5";

	private Double exchange_rate;

	@Override
	public Double getHBarValue() {
		if(exchange_rate == null)
			return null;
		return this.exchange_rate;
	}

	public Double getExchange_rate() {
		return exchange_rate;
	}

	public void setExchange_rate(Double exchange_rate) {
		this.exchange_rate = exchange_rate;
	}

	public static Liquid load() throws IOException {
		URL obj = new URL(LIQUID_URL);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		int responseCode = con.getResponseCode();

		final Liquid liquid =  OBJECT_MAPPER.readValue(con.getInputStream(), Liquid.class);
		con.disconnect();
		return liquid;
	}
}
