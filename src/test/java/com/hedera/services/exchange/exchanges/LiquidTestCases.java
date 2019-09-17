package com.hedera.services.exchange.exchanges;

import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LiquidTestCases {
    
    @Test
    public void retrieveLiquidDataTest() throws IOException {
        final String result = "{\"product_type\":\"CurrencyPair\", \"code\":\"CASH\", \"last_traded_price\": 0.0093}";
        final InputStream json = new ByteArrayInputStream(result.getBytes());
        final HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenReturn(json);
        new MockUp<Liquid>() {
            @Mock
            HttpURLConnection getConnection(final URL url) {
                return connection;
            }
        };

        final Liquid liquid = Liquid.load("https://api.liquid.com/products/5");
        assertEquals(0.0093, liquid.getHBarValue());
        assertEquals("CurrencyPair", liquid.getProductType());
        assertEquals("CASH", liquid.getCode());
    }
}
