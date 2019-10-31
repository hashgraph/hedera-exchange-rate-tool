package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * represents a OkCoin Exchange response.
 *
 * @author Anirudh, Cesar
 */
public final class OkCoin extends AbstractExchange {

    @JsonProperty(value="product_id", access = JsonProperty.Access.WRITE_ONLY)
    private String productid;

    @JsonProperty(value="instrument_id", access = JsonProperty.Access.WRITE_ONLY)
    private String instrumentid;

    @JsonProperty(value="last", access = JsonProperty.Access.WRITE_ONLY)
    private double rate;

    @Override
    @JsonProperty("HBAR")
    public Double getHBarValue() {
        return rate;
    }

    public String getProductid() {
        return productid;
    }

    public String getInstrumentid() {
        return instrumentid;
    }

    public static OkCoin load(final String endpoint) {
        return load(endpoint, OkCoin.class);
    }
}
