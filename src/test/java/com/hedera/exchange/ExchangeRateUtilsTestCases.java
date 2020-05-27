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
import com.hedera.exchange.exchanges.Exchange;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExchangeRateUtilsTestCases {


    @ParameterizedTest
    @CsvSource({"src/test/resources/configs/config.json"})
    public void weightedMedianCalculationTestWithSomeInvalidExchanges(final String configPath) throws Exception{

        this.setExchanges(  0.045, 526928.72830332,
                            0.043, 4678.8129,
                            0.0,4678.8129,
                            0.045, 0.5
        );
        final ERTParams params = ERTParams.readConfig(configPath);
        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                params.getExchangeAPIList(),
                params.getBound(),
                params.getFloor(),
                params.getDefaultRate(),
                params.getDefaultRate(),
                params.getFrequencyInSeconds());

        List<Exchange> exchanges = ertProcess.generateExchanges();

        Double median = ExchangeRateUtils.calculateMedianRate(exchanges);
        assertEquals(2, exchanges.size());
        assertEquals((Double) 0.045, median);
    }

    @ParameterizedTest
    @CsvSource({"src/test/resources/configs/config.json"})
    public void weightedMedianCalculationTest(final String configPath) throws Exception{

        this.setExchanges(  0.045, 1000.0,
                            0.043, 1000.0,
                            0.044,1000.0,
                            0.045, 1000.0
        );
        final ERTParams params = ERTParams.readConfig(configPath);
        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                params.getExchangeAPIList(),
                params.getBound(),
                params.getFloor(),
                params.getDefaultRate(),
                params.getDefaultRate(),
                params.getFrequencyInSeconds());

        List<Exchange> exchanges = ertProcess.generateExchanges();

        Double median = ExchangeRateUtils.calculateMedianRate(exchanges);
        assertEquals(4, exchanges.size());
        assertEquals((Double) 0.0445, median);
    }

    private void setExchanges(Double bittrexRate,
                              Double bittrexVolume,
                              Double liquidRate,
                              Double liquidVolume,
                              Double okcoinRate,
                              Double okcoinVolume,
                              Double binanceRate,
                              Double binanceVolume) throws IOException {
        final String bitrexResult = String.format("{\"success\":true,\"message\":\"Data Sent\",\"result\":" +
                "{\"Bid\":0.00952751,\"Ask\":0.00753996," +
                "\"Last\":%.5f,\"BaseVolume\":%.5f}}", bittrexRate, bittrexVolume);
        final InputStream bitrexJson = new ByteArrayInputStream(bitrexResult.getBytes());
        final HttpURLConnection bitrexConnection = mock(HttpURLConnection.class);
        when(bitrexConnection.getInputStream()).thenReturn(bitrexJson);

        final String liquidResult =  String.format("{\"product_type\":\"CurrencyPair\", \"code\":\"CASH\"," +
                " \"last_traded_price\": \"%.5f\", \"volume_24h\":\"%.5f\"}", liquidRate, liquidVolume);
        final InputStream liquidJson = new ByteArrayInputStream(liquidResult.getBytes());
        final HttpURLConnection liquidConnection = mock(HttpURLConnection.class);
        when(liquidConnection.getInputStream()).thenReturn(liquidJson);

        final String okCoinResult =  String.format("{\"product_id\": \"HBAR-USD\",\"instrument_id\": \"USD-USD\",\"last\": \"%.5f\"," +
                " \"quote_volume_24h\":\"%.5f\"}", okcoinRate, okcoinVolume);
        final InputStream okCoinJson = new ByteArrayInputStream(okCoinResult.getBytes());
        final HttpURLConnection okCoinConnection = mock(HttpURLConnection.class);
        when(okCoinConnection.getInputStream()).thenReturn(okCoinJson);

        final String binanceResult =  String.format("{\"symbol\": \"HBARUSD\",\"lastPrice\": %.5f," +
                " \"quoteVolume\":\"%.5f\"}", binanceRate, binanceVolume);
        final InputStream binanceJson = new ByteArrayInputStream(binanceResult.getBytes());
        final HttpURLConnection binanceConnection = mock(HttpURLConnection.class);
        when(binanceConnection.getInputStream()).thenReturn(binanceJson);
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

                if (host.contains("coinapi")) {
                    return okCoinConnection;
                }

                if (host.contains("binance")) {
                    return binanceConnection;
                }

                return null;
            }
        };
    }
}
