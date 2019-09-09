package com.hedera.services.exchange;

import com.hedera.hashgraph.sdk.proto.ExchangeRateSet;
import com.hedera.services.exchange.database.ExchangeDB;
import com.hedera.services.exchange.database.ExchangeRateAWSRD;
import com.hedera.services.exchange.exchanges.AbstractExchange;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ERTprocTestCases {

    @ParameterizedTest
    @CsvSource({"src/test/resources/configs/config.json,936,954",
                "src/test/resources/configs/config1.json,780,818",
                "src/test/resources/configs/config2.json,1200,1140"})
    public void testMedian(final String configPath, final int currentCentEquiv, final int expectedCentEquiv) throws Exception {
        this.setExchanges();

        final ERTParams params = ERTParams.readConfig(configPath);
        final ExchangeDB exchangeDb = params.getExchangeDB();
        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                params.getExchangeAPIList(),
                params.getMaxDelta(),
                params.getDefaultRate(),
                exchangeDb);
        final ExchangeRate exchangeRate = ertProcess.call();
        final ExchangeRateSet exchangeRateSet = exchangeRate.toExchangeRateSet();
        assertEquals(expectedCentEquiv, exchangeRateSet.getNextRate().getCentEquiv());
        assertEquals(1_000_000, exchangeRateSet.getNextRate().getHbarEquiv());
        final String expectedJson = String.format("[{"+
                "\"CurrentRate\":{\"hbarEquiv\":1000000,\"centEquiv\":%d,\"expirationTime\":%d}," +
                "\"NextRate\":{\"hbarEquiv\":1000000,\"centEquiv\":%d,\"expirationTime\":%d}}]",
                currentCentEquiv,
                exchangeRate.getCurrentExpiriationsTimeInSeconds(),
                expectedCentEquiv,
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

    @Test
    public void testHbarCentEquiv(){
        // change buildrate to static to make this work
        //Rate testRate = ERTproc.buildRate(0.0071, 1234567890);
        //System.out.println(testRate.toString());
    }
}
