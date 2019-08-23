package com.hedera.services.exchange;

import com.hedera.hashgraph.sdk.proto.ExchangeRateSet;
import com.hedera.services.exchange.exchanges.AbstractExchange;
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

        final ERTParams params = ERTParams.readConfig("src/test/resources/configs/config.json");
        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                params.getExchangeAPIList(),
                params.getMaxDelta(),
                params.getDefaultRate());
        final ExchangeRate exchangeRate = ertProcess.call();
        final ExchangeRateSet exchangeRateSet = exchangeRate.toExchangeRateSet();
        assertEquals(954, exchangeRateSet.getNextRate().getCentEquiv());
        assertEquals(100_000, exchangeRateSet.getNextRate().getHbarEquiv());
        final String expectedJson = String.format("{\"exchangeRate\":{" +
                "\"currentRate\":{\"hbarEquiv\":100000,\"centEquiv\":936,\"expirationTime\":{\"seconds\":%d}}," +
                "\"nextRate\":{\"hbarEquiv\":100000,\"centEquiv\":954,\"expirationTime\":{\"seconds\":%d}}}}",
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

                if (host.contains("coinbase")) {
                    return coinbaseConnection;
                }

                return null;
            }
        };
    }
}
