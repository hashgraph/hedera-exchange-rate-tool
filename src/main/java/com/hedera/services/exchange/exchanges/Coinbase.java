package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class Coinbase implements Exchange {

    private static final String COINBASE_URL = "https://api.coinbase.com/v2/exchange-rates";

    private static final Coinbase DEFAULT = new Coinbase();

    private static final URL url;

    static {
        try {
            url = new URL(COINBASE_URL);
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

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

    public static Coinbase load() {
        try {
            final HttpURLConnection con = getConnection();
            final Coinbase coinbase =  OBJECT_MAPPER.readValue(con.getInputStream(), Coinbase.class);
            con.disconnect();
            return coinbase;
        } catch (final Exception exception) {
            return DEFAULT;
        }
    }

    private static HttpURLConnection getConnection() throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    private static class Data {

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("rates")
        private Map<String, String> rates;
    }
}
