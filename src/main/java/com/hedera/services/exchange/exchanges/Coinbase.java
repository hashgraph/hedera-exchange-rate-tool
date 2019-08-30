package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Coinbase extends AbstractExchange{

    @JsonProperty("data")
    @JsonIgnore
    private Data data;

    @Override
    @JsonProperty("Response")
    public Double getHBarValue() {
        if (this.data == null || this.data.rates == null || !this.data.rates.containsKey("HBAR")) {
            return null;
        }

        return Double.valueOf(this.data.rates.get("HBAR"));
    }

    String getCurrency() {
        return this.data.currency;
    }

    public static Coinbase load(final String endpoint) {
        return load(endpoint, Coinbase.class);
    }

    private static class Data {

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("rates")
        private Map<String, String> rates;
    }
}
