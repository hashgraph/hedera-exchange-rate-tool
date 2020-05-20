package com.hedera.exchange;

/*-
 * ‌
 * Hedera Exchange Rate Tool
 * ​
 * Copyright (C) 2019 - 2020 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.hedera.exchange.exchanges.AbstractExchange;
import com.hedera.hashgraph.proto.ExchangeRateSet;
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
                params.getFloor(),
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
                exchangeRate.getCurrentExpirationsTimeInSeconds(),
                expectedCentEquiv,
                exchangeRate.getNextExpirationTimeInSeconds());
        assertEquals(expectedJson, exchangeRate.toJson());
    }

    @ParameterizedTest
    @CsvSource({"src/test/resources/configs/config.json,360000,286248",
            "src/test/resources/configs/config1.json,252000000,290124",
            "src/test/resources/configs/config2.json,25920000,290124"})
    public void testMedianWithNullMidnightValue(final String configPath, final int currentCentEquiv, final int expectedCentEquiv) throws Exception {
        this.setExchanges();

        final ERTParams params = ERTParams.readConfig(configPath);
        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                params.getExchangeAPIList(),
                params.getBound(),
                params.getFloor(),
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
                exchangeRate.getCurrentExpirationsTimeInSeconds(),
                expectedCentEquiv,
                exchangeRate.getNextExpirationTimeInSeconds());
        assertEquals(expectedJson, exchangeRate.toJson());
    }

    @ParameterizedTest
    @CsvSource({"src/test/resources/configs/configSimple.json,1,1000,1,1200,15.00",
                "src/test/resources/configs/configSimple.json,1,1000,1,834,6.00",
                "src/test/resources/configs/configSimple.json,1,1000,1,950,9.50",
                "src/test/resources/configs/configSimple.json,1,1000,1,1200,12.20",
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
                params.getFloor(),
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
                exchangeRate.getCurrentExpirationsTimeInSeconds(),
                expectedHBarEquiv,
                expectedCentEquiv,
                exchangeRate.getNextExpirationTimeInSeconds());
        assertEquals(expectedJson, exchangeRate.toJson());
        assertEquals(exchangesInJson, ertProcess.getExchangeJson());
    }

    @ParameterizedTest
    @CsvSource({"src/test/resources/configs/config.json,126000,120000",
                "src/test/resources/configs/config.json,150000,120000"})
    public void testFloor(String configPath, long currentCentEquiv, long expectedCentEquiv) throws IOException {
        this.setFloorExchanges();
        final ERTParams params = ERTParams.readConfig(configPath);
        final Rate currentRate = new Rate(30000, currentCentEquiv,ERTParams.getCurrentExpirationTime());
        final Rate midnightRate = new Rate(30000, currentCentEquiv,ERTParams.getCurrentExpirationTime());

        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                params.getExchangeAPIList(),
                params.getBound(),
                params.getFloor(),
                midnightRate,
                currentRate,
                params.getFrequencyInSeconds());
        final ExchangeRate exchangeRate = ertProcess.call();
        final ExchangeRateSet exchangeRateSet = exchangeRate.toExchangeRateSet();
        assertEquals(expectedCentEquiv, exchangeRateSet.getNextRate().getCentEquiv());
        assertEquals(30_000, exchangeRateSet.getNextRate().getHbarEquiv());
        final String expectedJson = String.format("[{"+
                        "\"CurrentRate\":{\"hbarEquiv\":30000,\"centEquiv\":%d,\"expirationTime\":%d}," +
                        "\"NextRate\":{\"hbarEquiv\":30000,\"centEquiv\":%d,\"expirationTime\":%d}}]",
                currentCentEquiv,
                exchangeRate.getCurrentExpirationsTimeInSeconds(),
                expectedCentEquiv,
                exchangeRate.getNextExpirationTimeInSeconds());
        assertEquals(expectedJson, exchangeRate.toJson());
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
                "\"Last\":0.0954162}}";
        final InputStream bitrexJson = new ByteArrayInputStream(bitrexResult.getBytes());
        final HttpURLConnection bitrexConnection = mock(HttpURLConnection.class);
        when(bitrexConnection.getInputStream()).thenReturn(bitrexJson);


        final String coinbaseResult = "{\"data\":{\"currency\":\"USD\", \"rates\":{\"HBAR\":\"0.098\"}}}";
        final InputStream coinbaseJson = new ByteArrayInputStream(coinbaseResult.getBytes());
        final HttpURLConnection coinbaseConnection = mock(HttpURLConnection.class);
        when(coinbaseConnection.getInputStream()).thenReturn(coinbaseJson);

        final String liquidResult = "{\"product_type\":\"CurrencyPair\", \"code\":\"CASH\", \"last_traded_price\": 0.093}";
        final InputStream liquidJson = new ByteArrayInputStream(liquidResult.getBytes());
        final HttpURLConnection liquidConnection = mock(HttpURLConnection.class);
        when(liquidConnection.getInputStream()).thenReturn(liquidJson);

        final String okCoinResult = "{\"product_id\": \"HBAR-USD\",\"instrument_id\": \"USD-USD\",\"last\": 0.093}";
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

    private void setFloorExchanges() throws IOException {
        final String bitrexResult = "{\"success\":true,\"message\":\"Data Sent\",\"result\":{\"Bid\":0.00952751,\"Ask\":0.0553996," +
                "\"Last\":0.0354162}}";
        final InputStream bitrexJson = new ByteArrayInputStream(bitrexResult.getBytes());
        final HttpURLConnection bitrexConnection = mock(HttpURLConnection.class);
        when(bitrexConnection.getInputStream()).thenReturn(bitrexJson);


        final String coinbaseResult = "{\"data\":{\"currency\":\"USD\", \"rates\":{\"HBAR\":\"0.038\"}}}";
        final InputStream coinbaseJson = new ByteArrayInputStream(coinbaseResult.getBytes());
        final HttpURLConnection coinbaseConnection = mock(HttpURLConnection.class);
        when(coinbaseConnection.getInputStream()).thenReturn(coinbaseJson);

        final String liquidResult = "{\"product_type\":\"CurrencyPair\", \"code\":\"CASH\", \"last_traded_price\": 0.033}";
        final InputStream liquidJson = new ByteArrayInputStream(liquidResult.getBytes());
        final HttpURLConnection liquidConnection = mock(HttpURLConnection.class);
        when(liquidConnection.getInputStream()).thenReturn(liquidJson);

        final String okCoinResult = "{\"product_id\": \"HBAR-USD\",\"instrument_id\": \"USD-USD\",\"last\": 0.033}";
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
