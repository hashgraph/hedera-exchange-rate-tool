package com.hedera.services.exchange.exchanges;

import java.net.MalformedURLException;
import java.net.URL;

public class UpBit extends AbstractExchange {


    private String endPoint;

    @Override
    public void setEndPoint(String url) {
        this.endPoint = url;
    }

    @Override
    public String getEndPoint(){
        return this.endPoint;
    }

    @Override
    public Double getHBarValue() {
        return null;
    }

    public static UpBit load(final String endpoint) {
        return load(endpoint, UpBit.class);
    }
}
