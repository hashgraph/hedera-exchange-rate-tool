package com.hedera.services.exchange;

import com.hedera.hashgraph.sdk.proto.ExchangeRateSet;
import com.hedera.services.exchange.exchanges.Bitrex;
import com.hedera.services.exchange.exchanges.Coinbase;
import com.hedera.services.exchange.exchanges.Liquid;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ERTprocTestCases {

    public ERTproc ertProcess = new ERTproc("0", null, "0", "0", 5.0,
            0.0091600, 2600, "0");

    @Test
    public void testMedian() throws IOException {
        this.setExchanges();

        final ExchangeRate exchangeRate = ertProcess.call();
        final ExchangeRateSet exchangeRateSet = exchangeRate.toExchangeRateSet();
        assertEquals(954, exchangeRateSet.getNextRate().getCentEquiv());
        assertEquals(100_000, exchangeRateSet.getNextRate().getHbarEquiv());
        assertEquals("{\"currentRate\":{\"expirationTimeInSeconds\":1566237600,\"hbarEquiv\":100000,\"centEquiv\":0," +
                "\"expirationTime\":{\"seconds\":0}}," +
                "\"nextRate\":{\"expirationTimeInSeconds\":3600," +
                "\"hbarEquiv\":100000,\"centEquiv\":954,\"expirationTime\":{\"seconds\":3600}}}", exchangeRate.toJson());
    }

    private void setExchanges() throws IOException {
        final String bitrexResult = "{\"success\":true,\"message\":\"Data Sent\",\"result\":{\"Bid\":0.00952751,\"Ask\":0.00753996," +
                "\"Last\":0.00954162}}";
        final InputStream bitrexJson = new ByteArrayInputStream(bitrexResult.getBytes());
        final HttpURLConnection bitrexConnection = mock(HttpURLConnection.class);
        when(bitrexConnection.getInputStream()).thenReturn(bitrexJson);
        new MockUp<Bitrex>() {
            @Mock
            HttpURLConnection getConnection() {
                return bitrexConnection;
            }
        };

        final String coinbaseResult = "{\"data\":{\"currency\":\"USD\", \"rates\":{\"HBAR\":\"0.0098\"}}}";
        final InputStream coinbaseJson = new ByteArrayInputStream(coinbaseResult.getBytes());
        final HttpURLConnection coinbaseConnection = mock(HttpURLConnection.class);
        when(coinbaseConnection.getInputStream()).thenReturn(coinbaseJson);
        new MockUp<Coinbase>() {
            @Mock
            HttpURLConnection getConnection() {
                return coinbaseConnection;
            }
        };

        final String liquidResult = "{\"product_type\":\"CurrencyPair\", \"code\":\"CASH\", \"exchange_rate\": 0.0093}";
        final InputStream liquidJson = new ByteArrayInputStream(liquidResult.getBytes());
        final HttpURLConnection liquidConnection = mock(HttpURLConnection.class);
        when(liquidConnection.getInputStream()).thenReturn(liquidJson);
        new MockUp<Liquid>() {
            @Mock
            HttpURLConnection getConnection() {
                return liquidConnection;
            }
        };
    }
}
