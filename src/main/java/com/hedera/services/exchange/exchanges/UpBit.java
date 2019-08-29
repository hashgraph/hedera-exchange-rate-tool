package com.hedera.services.exchange.exchanges;

import java.net.MalformedURLException;
import java.net.URL;

public class UpBit extends AbstractExchange {


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
        return null;
    }

    public static UpBit load(final String endpoint) {
        return load(endpoint, UpBit.class);
    }
}
