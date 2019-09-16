package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class OkCoin extends AbstractExchange {

    public static final String APIKEY = "A1E5E418-E7CA-4A6A-B2CC-01D6BF3772B9";

    @JsonProperty(value="asset_id_base", access = JsonProperty.Access.WRITE_ONLY)
    private String base;

    @JsonProperty(value="asset_id_quote", access = JsonProperty.Access.WRITE_ONLY)
    private String quote;

    @JsonProperty(value="rate", access = JsonProperty.Access.WRITE_ONLY)
    private double rate;

    @Override
    public Double getHBarValue() {
        return rate;
    }

    public String getBase() {
        return base;
    }

    public String getQuote() {
        return quote;
    }

    public static OkCoin load(final String endpoint) {
        return load(endpoint, OkCoin.class);
    }
}
