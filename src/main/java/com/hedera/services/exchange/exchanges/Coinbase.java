package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Coinbase extends AbstractExchange{

    @JsonProperty("data")
    private Data data;

    private String response;

    private String endPoint;

    @Override
    public String getResponse(){
        return String.format("\"Query:{}\",\"Response:{}\";",endPoint,response);
    }

    @Override
    public void setEndPoint(String url) {
        this.endPoint = url;
    }

    @Override
    public void setResponse(String response){
        this.response = response;
    }

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
