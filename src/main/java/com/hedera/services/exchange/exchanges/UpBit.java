package com.hedera.services.exchange.exchanges;

import java.net.MalformedURLException;
import java.net.URL;

public class UpBit implements Exchange {

    private static String UPBIT_URL = "https://api.coinbase.com/v2/exchange-rates";

    private static final UpBit DEFAULT = new UpBit();

    private static final URL url;

    static {
        try {
            url = new URL(UPBIT_URL);
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Double getHBarValue() {
        return null;
    }

    public static String getUpbitUrl() {
        return UPBIT_URL;
    }

    public static void setUpbitUrl(String upbitUrl) {
        UPBIT_URL = upbitUrl;
    }
}
