package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Coinbase extends AbstractExchange{

    // TODO update the URL
    public static final String COINBASE_URL = "https://api.coinbase.com/v2/exchange-rates";

    private static final Coinbase DEFAULT = new Coinbase();

    @JsonProperty("data")
    private Data data;

    @Override
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
