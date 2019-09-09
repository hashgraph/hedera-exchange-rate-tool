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

public class CoinbaseTestCases {

    @Test
    public void retrieveCoinbaseDataTest() throws IOException {
        final String result = "{\"data\":{\"currency\":\"USD\", \"rates\":{\"HBAR\":\"0.0098\"}}}";
        final InputStream json = new ByteArrayInputStream(result.getBytes());
        final HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenReturn(json);
        new MockUp<Coinbase>() {
            @Mock
            HttpURLConnection getConnection(final URL url) {
                return connection;
            }
        };

        final Coinbase coinbase = Coinbase.load("https://api.coinbase.com/v2/exchange-rates");
        assertEquals("USD", coinbase.getCurrency() );
        assertEquals(0.0098, coinbase.getHBarValue());
        assertEquals("{\"Query\":\"https://api.coinbase.com/v2/exchange-rates\",\"HBAR\":0.0098}", coinbase.toJson());
    }
}
