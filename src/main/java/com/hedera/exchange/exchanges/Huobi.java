package com.hedera.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Huobi extends ExchangeCoin{

	@JsonProperty(value="status", access = JsonProperty.Access.WRITE_ONLY)
	private String status;

	@JsonProperty(value="tick", access = JsonProperty.Access.WRITE_ONLY)
	private TickerData tickerData;

	@Override
	public Double getHBarValue() {
		return tickerData == null ? null : tickerData.price;
	}

	@Override
	public Double getVolume() {
		return tickerData == null || tickerData.volume == null || tickerData.volume <= 1.0 ?
				0.0 : tickerData.volume;
	}

	public String getStatus() {
		return status;
	}

	private static class TickerData {
		@JsonProperty(value="close")
		private Double price;

		@JsonProperty(value="vol")
		private Double volume;
	}
}
