package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hedera.services.exchange.ERTproc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class Coinbase extends AbstractExchange{
    private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);


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
        LOGGER.debug("Loading exchange rate form Coinbase");
        return load(endpoint, Coinbase.class);
    }

    private static class Data {

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("rates")
        private Map<String, String> rates;
    }
}
