package com.hedera.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Gate extends ExchangeCoin{
	@JsonProperty(value="last", access = JsonProperty.Access.WRITE_ONLY)
	private Double last;

	@JsonProperty(value="base_volume", access = JsonProperty.Access.WRITE_ONLY)
	private Double volume;

	@Override
	public Double getHBarValue() {
		return last;
	}

	@Override
	public Double getVolume() {
		if (volume == null || volume <= 1.0) {
			return 0.0;
		}

		return volume;
	}
}
