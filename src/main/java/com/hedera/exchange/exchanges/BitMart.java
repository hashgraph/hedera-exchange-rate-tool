package com.hedera.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a BitMart Exchange response.
 *
 * @author Anirudh, Cesar
 */
public class BitMart extends ExchangeCoin {

	@JsonProperty(value="message", access = JsonProperty.Access.WRITE_ONLY)
	private String message;

	@JsonProperty(value="data", access = JsonProperty.Access.WRITE_ONLY)
	private BitMartTickerData tickerData;


	@Override
	public Double getHBarValue() {
		if (tickerData == null || tickerData.data[0].last == null) {
			return null;
		}

		return this.tickerData.data[0].last;
	}

	@Override
	public Double getVolume() {
		if (tickerData == null || tickerData.data[0].volume == null || tickerData.data[0].volume <= 1.0) {
			return 0.0;
		}

		return this.tickerData.data[0].volume;
	}

	public String getMessage() {
		return message;
	}

	private static class BitMartTickerData{
		@JsonProperty("tickers")
		private TickerData[] data;
	}

	private static class TickerData {
		@JsonProperty("last_price")
		private Double last;

		@JsonProperty("volume_24h")
		private Double volume;
	}
}
