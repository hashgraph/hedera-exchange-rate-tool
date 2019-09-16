package com.hedera.services.exchange.exchanges;

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
        final String result = "{\"asset_id_base\":\"HBAR\", \"asset_id_quote\":\"USD\", \"rate\": 0.008754}";
        final InputStream json = new ByteArrayInputStream(result.getBytes());
        final HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenReturn(json);
        new MockUp<Liquid>() {
            @Mock
            HttpURLConnection getConnection(final URL url) {
                return connection;
            }
        };

        final OkCoin okcoin = OkCoin.load("https://rest.coinapi.io/v1/exchangerate/HBAR/USD");
        assertEquals(0.008754, okcoin.getHBarValue());
        assertEquals("USD", okcoin.getQuote());
        assertEquals("HBAR", okcoin.getBase());
    }

    @Test
    public void fetchOkCoinWithNullResultTest() throws IOException {
        final String result = "{\"asset_id_base\":null, \"asset_id_quote\":\"USD\", \"rate\": null}";
        final InputStream json = new ByteArrayInputStream(result.getBytes());
        final HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenReturn(json);
        new MockUp<Bitrex>() {
            @Mock
            HttpURLConnection getConnection(final URL url) {
                return connection;
            }
        };

        final OkCoin okcoin = OkCoin.load("https://rest.coinapi.io/v1/exchangerate/HBAR/USD");
        assertEquals("USD", okcoin.getQuote());
        assertEquals(0.0, okcoin.getHBarValue());
        assertNull(okcoin.getBase());
    }
}