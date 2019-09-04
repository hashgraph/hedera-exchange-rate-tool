package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.MalformedURLException;
import java.net.URL;

public class UpBit extends AbstractExchange {

    @JsonProperty("Query")
    String endPoint = "";

    @Override
    public void setEndPoint(String url) {
        endPoint = url;
    }

    @Override
    @JsonProperty("HBAR")
    public Double getHBarValue() {
        return null;
    }

    public static UpBit load(final String endpoint) {
        return load(endpoint, UpBit.class);
    }
}
