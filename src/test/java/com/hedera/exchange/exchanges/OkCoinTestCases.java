package com.hedera.exchange.exchanges;

import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OkCoinTestCases {
    @Test
    public void retrieveOkCoinDataTest() throws IOException {
        final String result = "{\"product_id\":\"HBAR-USD\", \"instrument_id\":\"HBAR-USD\", \"last\": 0.008754}";
        final InputStream json = new ByteArrayInputStream(result.getBytes());
        final HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenReturn(json);
        new MockUp<Liquid>() {
            @Mock
            HttpURLConnection getConnection(final URL url) {
                return connection;
            }
        };

        final OkCoin okcoin = OkCoin.load("https://www.okcoin.com/api/spot/v3/instruments/HBAR-USD/ticker");
        assertEquals(0.008754, okcoin.getHBarValue());
        assertEquals("HBAR-USD", okcoin.getInstrumentid());
        assertEquals("HBAR-USD", okcoin.getProductid());
    }

    @Test
    public void fetchOkCoinWithNullResultTest() throws IOException {
        final String result = "{\"product_id\":null, \"instrument_id\":\"HBAR-USD\", \"last\": null}";
        final InputStream json = new ByteArrayInputStream(result.getBytes());
        final HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenReturn(json);
        new MockUp<Bitrex>() {
            @Mock
            HttpURLConnection getConnection(final URL url) {
                return connection;
            }
        };

        final OkCoin okcoin = OkCoin.load("https://www.okcoin.com/api/spot/v3/instruments/HBAR-USD/ticker");
        assertEquals("HBAR-USD", okcoin.getInstrumentid());
        assertEquals(0.0, okcoin.getHBarValue());
        assertNull(okcoin.getProductid());
    }
}
