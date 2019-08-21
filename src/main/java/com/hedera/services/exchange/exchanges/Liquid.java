package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hedera.services.exchange.ERTproc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Liquid extends AbstractExchange {
	private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);


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

	public static Liquid load(final String endpoint) {
		LOGGER.debug("Loading exchange rate from Liquid");
		return load(endpoint, Liquid.class);
	}
}
