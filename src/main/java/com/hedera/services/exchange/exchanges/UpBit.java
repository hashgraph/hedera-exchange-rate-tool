package com.hedera.services.exchange.exchanges;

import java.net.MalformedURLException;
import java.net.URL;

public class UpBit extends AbstractExchange {


    @Override
    public Double getHBarValue() {
        return null;
    }

    public static UpBit load(final String endpoint) {
        return load(endpoint, UpBit.class);
    }
}
