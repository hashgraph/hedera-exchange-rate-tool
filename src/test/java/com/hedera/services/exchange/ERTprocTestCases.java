package com.hedera.services.exchange;

import com.hedera.hashgraph.sdk.proto.ExchangeRateSet;
import com.hedera.services.exchange.exchanges.AbstractExchange;
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
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ERTprocTestCases {
    @Test
    public void testMedian() throws Exception {
        this.setExchanges();

        final ERTParams params = ERTParams.readConfig();
        final ERTproc ertProcess = new ERTproc("0",
                params.getExchangeAPIList(),
                "0",
                params.getMaxDelta(),
                0.0091600,
                2600,
                "0");
        final ExchangeRate exchangeRate = ertProcess.call();
        final ExchangeRateSet exchangeRateSet = exchangeRate.toExchangeRateSet();
        assertEquals(954, exchangeRateSet.getNextRate().getCentEquiv());
        assertEquals(100_000, exchangeRateSet.getNextRate().getHbarEquiv());
        final String expectedJson = String.format("{" +
                "\"currentRate\":{\"hbarEquiv\":100000,\"centEquiv\":916,\"expirationTime\":{\"seconds\":%d}}," +
                "\"nextRate\":{\"hbarEquiv\":100000,\"centEquiv\":954,\"expirationTime\":{\"seconds\":%d}}}",
                exchangeRate.getCurrentExpiriationsTimeInSeconds(),
                exchangeRate.getNextExpirationTimeInSeconds());
        assertEquals(expectedJson, exchangeRate.toJson());
    }

    private void setExchanges() throws IOException {
        final String bitrexResult = "{\"success\":true,\"message\":\"Data Sent\",\"result\":{\"Bid\":0.00952751,\"Ask\":0.00753996," +
                "\"Last\":0.00954162}}";
        final InputStream bitrexJson = new ByteArrayInputStream(bitrexResult.getBytes());
        final HttpURLConnection bitrexConnection = mock(HttpURLConnection.class);
        when(bitrexConnection.getInputStream()).thenReturn(bitrexJson);


        final String coinbaseResult = "{\"data\":{\"currency\":\"USD\", \"rates\":{\"HBAR\":\"0.0098\"}}}";
        final InputStream coinbaseJson = new ByteArrayInputStream(coinbaseResult.getBytes());
        final HttpURLConnection coinbaseConnection = mock(HttpURLConnection.class);
        when(coinbaseConnection.getInputStream()).thenReturn(coinbaseJson);

        final String liquidResult = "{\"product_type\":\"CurrencyPair\", \"code\":\"CASH\", \"exchange_rate\": 0.0093}";
        final InputStream liquidJson = new ByteArrayInputStream(liquidResult.getBytes());
        final HttpURLConnection liquidConnection = mock(HttpURLConnection.class);
        when(liquidConnection.getInputStream()).thenReturn(liquidJson);
        new MockUp<AbstractExchange>() {
            @Mock
            HttpURLConnection getConnection(final URL url) {
                final String host = url.getHost();
                if (host.contains("bittrex")) {
                    return bitrexConnection;
                }

                if (host.contains("liquid")) {
                    return liquidConnection;
                }

                return coinbaseConnection;
            }
        };
    }
}
