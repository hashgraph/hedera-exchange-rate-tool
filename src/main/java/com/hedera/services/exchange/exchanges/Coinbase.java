package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class Coinbase implements Exchange{

    private static final String COINBASE_URL = "https://api.coinbase.com/v2/exchange-rates ";

    // TODO to test this we can change this or add another variable BTC to see the data
    // TODO right now there is no HBAR.
    @JsonProperty("HBAR")
    private Double hbar;

    @Override
    public Double getHBarValue() {
        if(hbar == null)
            return null;
        return this.hbar;
    }

    public Double getHbar() {
        return hbar;
    }

    public void setHbar(Double hbar) {
        this.hbar = hbar;
    }

    public static Coinbase load() throws IOException {
        URL obj = new URL(COINBASE_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        final Coinbase coinbase =  OBJECT_MAPPER.readValue(con.getInputStream(), Coinbase.class);
        con.disconnect();
        return coinbase;
    }
}
