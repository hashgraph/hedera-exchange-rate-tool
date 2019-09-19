package com.hedera.services.exchange;

import com.hedera.hashgraph.sdk.proto.ExchangeRateSet;
import com.hedera.services.exchange.database.ExchangeDB;
import com.hedera.services.exchange.exchanges.AbstractExchange;
import mockit.Mock;
import mockit.MockUp;
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
    @CsvSource({"src/test/resources/configs/config.json,360000,288000",
                "src/test/resources/configs/config1.json,252000000,201600000",
                "src/test/resources/configs/config2.json,25920000,20736000"})
    public void testMedianWithDefaultAsMidnight(final String configPath, final int currentCentEquiv, final int expectedCentEquiv) throws Exception {
        this.setExchanges();

        final ERTParams params = ERTParams.readConfig(configPath);
        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                params.getExchangeAPIList(),
                params.getBound(),
                params.getDefaultRate(),
                params.getDefaultRate(),
                params.getFrequencyInSeconds());
        final ExchangeRate exchangeRate = ertProcess.call();
        final ExchangeRateSet exchangeRateSet = exchangeRate.toExchangeRateSet();
        assertEquals(expectedCentEquiv, exchangeRateSet.getNextRate().getCentEquiv());
        assertEquals(30_000, exchangeRateSet.getNextRate().getHbarEquiv());
        final String expectedJson = String.format("[{"+
                "\"CurrentRate\":{\"hbarEquiv\":30000,\"centEquiv\":%d,\"expirationTime\":%d}," +
                "\"NextRate\":{\"hbarEquiv\":30000,\"centEquiv\":%d,\"expirationTime\":%d}}]",
                currentCentEquiv,
                exchangeRate.getCurrentExpiriationsTimeInSeconds(),
                expectedCentEquiv,
                exchangeRate.getNextExpirationTimeInSeconds());
        assertEquals(expectedJson, exchangeRate.toJson());
    }

    @ParameterizedTest
    @CsvSource({"src/test/resources/configs/config.json,360000,28262",
            "src/test/resources/configs/config1.json,252000000,29012",
            "src/test/resources/configs/config2.json,25920000,29012"})
    public void testMedianWithNullMidnightValue(final String configPath, final int currentCentEquiv, final int expectedCentEquiv) throws Exception {
        this.setExchanges();

        final ERTParams params = ERTParams.readConfig(configPath);
        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                params.getExchangeAPIList(),
                params.getBound(),
                null,
                params.getDefaultRate(),
                params.getFrequencyInSeconds());
        final ExchangeRate exchangeRate = ertProcess.call();
        final ExchangeRateSet exchangeRateSet = exchangeRate.toExchangeRateSet();
        assertEquals(expectedCentEquiv, exchangeRateSet.getNextRate().getCentEquiv());
        assertEquals(30_000, exchangeRateSet.getNextRate().getHbarEquiv());
        final String expectedJson = String.format("[{"+
                        "\"CurrentRate\":{\"hbarEquiv\":30000,\"centEquiv\":%d,\"expirationTime\":%d}," +
                        "\"NextRate\":{\"hbarEquiv\":30000,\"centEquiv\":%d,\"expirationTime\":%d}}]",
                currentCentEquiv,
                exchangeRate.getCurrentExpiriationsTimeInSeconds(),
                expectedCentEquiv,
                exchangeRate.getNextExpirationTimeInSeconds());
        assertEquals(expectedJson, exchangeRate.toJson());
    }

    @ParameterizedTest
    @CsvSource({"src/test/resources/configs/configSimple.json,1,1000,1,1250,15.00",
                "src/test/resources/configs/configSimple.json,1,1000,1,800,6.00",
                "src/test/resources/configs/configSimple.json,1,1000,1,950,9.50",
                "src/test/resources/configs/configSimple.json,1,1000,1,1220,12.20",
                "src/test/resources/configs/configSimple.json,1,1000,1,1000,0.0"})
    public void testMedianWithCurrentRateSameAsRate(final String configPath,
            final long currentHBarEquiv,
            final long currentCentEquiv,
            final long expectedHBarEquiv,
            final long expectedCentEquiv,
            final double bitrexValue) throws Exception {
        this.setOnlyBitrex(bitrexValue);
        final String exchangesInJson = bitrexValue == 0.0 ? "[]" : String.format("[{\"Query\":\"https://api.bittrex.com/api/v1" +
                ".1/public/getticker?market=USD-HBAR\",\"HBAR\":%.1f}]", bitrexValue);
        final long currentExpirationInSeconds = ERTParams.getCurrentExpirationTime();
        final Rate currentRate = new Rate(currentHBarEquiv, currentCentEquiv, currentExpirationInSeconds);
        final Rate expectedRate = new Rate(expectedHBarEquiv, expectedCentEquiv, currentExpirationInSeconds + 3_600);

        final ERTParams params = ERTParams.readConfig(configPath);
        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                params.getExchangeAPIList(),
                params.getBound(),
                currentRate,
                currentRate,
                params.getFrequencyInSeconds());

        final ExchangeRate exchangeRate = ertProcess.call();

        final ExchangeRateSet exchangeRateSet = exchangeRate.toExchangeRateSet();
        assertEquals(expectedRate.getCentEquiv(), exchangeRateSet.getNextRate().getCentEquiv());
        assertEquals(expectedRate.getHBarEquiv(), exchangeRateSet.getNextRate().getHbarEquiv());
        final String expectedJson = String.format("[{"+
                        "\"CurrentRate\":{\"hbarEquiv\":%d,\"centEquiv\":%d,\"expirationTime\":%d}," +
                        "\"NextRate\":{\"hbarEquiv\":%d,\"centEquiv\":%d,\"expirationTime\":%d}}]",
                currentHBarEquiv,
                currentCentEquiv,
                exchangeRate.getCurrentExpiriationsTimeInSeconds(),
                expectedHBarEquiv,
                expectedCentEquiv,
                exchangeRate.getNextExpirationTimeInSeconds());
        assertEquals(expectedJson, exchangeRate.toJson());
        assertEquals(exchangesInJson, ertProcess.getExchangeJson());
    }

    private void setOnlyBitrex(final double value) throws IOException {
        final String bitrexResult = String.format("{\"success\":true,\"message\":\"Data Sent\",\"result\":{\"Bid\":0.00952751,\"Ask\":0.00753996," +
                "\"Last\":%.8f}}", value);
        final InputStream bitrexJson = new ByteArrayInputStream(bitrexResult.getBytes());
        final HttpURLConnection bitrexConnection = mock(HttpURLConnection.class);
        when(bitrexConnection.getInputStream()).thenReturn(bitrexJson);

        new MockUp<AbstractExchange>() {
            @Mock
            HttpURLConnection getConnection(final URL url) {
                final String host = url.getHost();
                if (host.contains("bittrex")) {
                    return bitrexConnection;
                }

                return null;
            }
        };
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

        final String liquidResult = "{\"product_type\":\"CurrencyPair\", \"code\":\"CASH\", \"last_traded_price\": 0.0093}";
        final InputStream liquidJson = new ByteArrayInputStream(liquidResult.getBytes());
        final HttpURLConnection liquidConnection = mock(HttpURLConnection.class);
        when(liquidConnection.getInputStream()).thenReturn(liquidJson);

        final String okCoinResult = "{\"asset_id_base\": \"HBAR\",\"asset_id_quote\": \"USD\",\"rate\": 0.0093}";
        final InputStream okCoinJson = new ByteArrayInputStream(okCoinResult.getBytes());
        final HttpURLConnection okCoinConnection = mock(HttpURLConnection.class);
        when(okCoinConnection.getInputStream()).thenReturn(okCoinJson);
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

                if (host.contains("coinapi")) {
                    return okCoinConnection;
                }

                return null;
            }
        };
    }
}
