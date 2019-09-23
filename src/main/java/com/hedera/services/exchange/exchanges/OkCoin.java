package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class OkCoin extends AbstractExchange {

    public static final String APIKEY = "A1E5E418-E7CA-4A6A-B2CC-01D6BF3772B9";

    @JsonProperty(value="product_id", access = JsonProperty.Access.WRITE_ONLY)
    private String product_id;

    @JsonProperty(value="instrument_id", access = JsonProperty.Access.WRITE_ONLY)
    private String instrument_id;

    @JsonProperty(value="last", access = JsonProperty.Access.WRITE_ONLY)
    private double rate;

    @Override
    public Double getHBarValue() {
        return rate;
    }

    public String getProduct_id() {
        return product_id;
    }

    public String getInstrument_id() {
        return instrument_id;
    }

    public static OkCoin load(final String endpoint) {
        return load(endpoint, OkCoin.class);
    }
}
