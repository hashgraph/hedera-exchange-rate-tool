package com.hedera.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public abstract class ExchangeCoin implements Exchange {

	@JsonProperty("Query")
	String endPoint = "";

	@Override
	public void setEndPoint(String url) {
		this.endPoint = url;
	}

	public String toJson() throws JsonProcessingException {
		return OBJECT_MAPPER.writeValueAsString(this);
	}
}
