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

import com.hedera.exchange.exchanges.Exchange;
import com.hedera.exchange.exchanges.Bitrex;
import com.hedera.exchange.exchanges.Binance;
import com.hedera.exchange.exchanges.Coinbase;
import com.hedera.exchange.exchanges.Liquid;
import com.hedera.exchange.exchanges.OkCoin;
import com.hedera.hashgraph.proto.ExchangeRateSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ERTprocTestCases {

    @ParameterizedTest
    @CsvSource({"src/test/resources/configs/config.json,360000,288000",
                "src/test/resources/configs/config1.json,252000000,201600000",
                "src/test/resources/configs/config2.json,25920000,20736000"})
    public void testMedianWithDefaultAsMidnight(final String configPath, final int currentCentEquiv, final int expectedCentEquiv) throws Exception {
        List<Exchange> exchanges = this.setExchanges();

        final ERTParams params = ERTParams.readConfig(configPath);
        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                exchanges,
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
    @CsvSource({"src/test/resources/configs/config.json,360000,285000",
            "src/test/resources/configs/config1.json,252000000,285000",
            "src/test/resources/configs/config2.json,25920000,285000"})
    public void testMedianWithNullMidnightValue(final String configPath, final int currentCentEquiv, final int expectedCentEquiv) throws Exception {
        List<Exchange> exchanges = this.setExchanges();

        final ERTParams params = ERTParams.readConfig(configPath);
        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                exchanges,
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
        List<Exchange> justBitrexExchange = this.setOnlyBitrex(bitrexValue);
        final String exchangesInJson = bitrexValue == 0.0 ? "[]" : String.format("[{\"volume\":1000.0," +
                "\"Query\":\"https://api.bittrex.com/api/v1.1/public/getticker?market=USD-HBAR\"," +
                "\"HBAR\":%.1f}]", bitrexValue);
        final long currentExpirationInSeconds = ERTParams.getCurrentExpirationTime();
        final Rate currentRate = new Rate(currentHBarEquiv, currentCentEquiv, currentExpirationInSeconds);
        final Rate expectedRate = new Rate(expectedHBarEquiv, expectedCentEquiv, currentExpirationInSeconds + 3_600);

        final ERTParams params = ERTParams.readConfig(configPath);
        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                justBitrexExchange,
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
    }

    @ParameterizedTest
    @CsvSource({"src/test/resources/configs/config.json,126000,120000",
                "src/test/resources/configs/config.json,150000,120000"})
    public void testFloor(String configPath, long currentCentEquiv, long expectedCentEquiv) throws IOException, IllegalAccessException, InstantiationException {
        List<Exchange> exchanges = this.setFloorExchanges();
        final ERTParams params = ERTParams.readConfig(configPath);
        final Rate currentRate = new Rate(30000, currentCentEquiv,ERTParams.getCurrentExpirationTime());
        final Rate midnightRate = new Rate(30000, currentCentEquiv,ERTParams.getCurrentExpirationTime());

        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                exchanges,
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

    @Test
    public void testWeightedMedian() throws Exception {
        final ERTParams params = ERTParams.readConfig("src/test/resources/configs/config.json");
        List<Exchange> emptyList = new ArrayList<>();
        final ERTproc ertProcess = new ERTproc(params.getDefaultHbarEquiv(),
                emptyList,
                params.getBound(),
                params.getFloor(),
                null,
                null,
                params.getFrequencyInSeconds()
        );

        assertEquals(1.0, ertProcess.findVolumeWeightedMedian(
                new double[]{0.0, 1.0, 2.0} , new double[]{1.0, 1.0, 1.0}));
        assertEquals(1.0, ertProcess.findVolumeWeightedMedian(
                new double[]{0.0, 1.0, 2.0} , new double[]{1.0, 0.0, 1.0}));
        assertEquals(2.0, ertProcess.findVolumeWeightedMedian(
                new double[]{0.0, 1.0, 2.0} , new double[]{0.0, 0.0, 1.0}));
        assertEquals(2.0, ertProcess.findVolumeWeightedMedian(
                new double[]{0.0, 1.0, 2.0, 3.0} , new double[]{1.0, 0.0, 0.0, 1.0}));
        assertEquals(3.0, ertProcess.findVolumeWeightedMedian(
                new double[]{0.0, 1.0, 2.0, 3.0, 4.0} , new double[]{1.0, 0.0, 0.0, 0.0, 1.0}));
        assertEquals(1.5, ertProcess.findVolumeWeightedMedian(
                new double[]{0.0, 1.0, 2.0, 3.0} , new double[]{1.0, 0.1, 0.1, 1.0}));
    }


    private List<Exchange> setOnlyBitrex(final double value) throws IOException {

        Bitrex mockBitrex = mock(Bitrex.class);
        when(mockBitrex.getHBarValue()).thenReturn(value);
        when(mockBitrex.getVolume()).thenReturn(1000.0);

        List<Exchange> exchanges = new ArrayList<>();
        exchanges.add(mockBitrex);

        return exchanges;
    }

    private List<Exchange> setExchanges() throws IOException {

        Bitrex mockBitrex = mock(Bitrex.class);
        when(mockBitrex.getHBarValue()).thenReturn(0.0954162);
        when(mockBitrex.getVolume()).thenReturn(1000.0);

        Coinbase mockCoinbase = mock(Coinbase.class);
        when(mockCoinbase.getHBarValue()).thenReturn(0.098);
        when(mockCoinbase.getVolume()).thenReturn(1000.0);

        Liquid mockLiquid = mock(Liquid.class);
        when(mockLiquid.getHBarValue()).thenReturn(0.093);
        when(mockLiquid.getVolume()).thenReturn(1000.0);

        OkCoin mockOkCoin = mock(OkCoin.class);
        when(mockOkCoin.getHBarValue()).thenReturn(0.093);
        when(mockOkCoin.getVolume()).thenReturn(1000.0);

        Binance mockBinance = mock(Binance.class);
        when(mockBinance.getHBarValue()).thenReturn(0.095);
        when(mockBinance.getVolume()).thenReturn(1000.0);

        List<Exchange> exchanges = new ArrayList<>();
        exchanges.add(mockBinance);
        exchanges.add(mockCoinbase);
        exchanges.add(mockBitrex);
        exchanges.add(mockLiquid);
        exchanges.add(mockOkCoin);

        return exchanges;

    }

    private List<Exchange> setFloorExchanges() throws IOException, InstantiationException, IllegalAccessException {

        Bitrex mockBitrex = mock(Bitrex.class);
        when(mockBitrex.getHBarValue()).thenReturn(0.0354162);
        when(mockBitrex.getVolume()).thenReturn(1000.0);

        Coinbase mockCoinbase = mock(Coinbase.class);
        when(mockCoinbase.getHBarValue()).thenReturn(0.038);
        when(mockCoinbase.getVolume()).thenReturn(1000.0);

        Liquid mockLiquid = mock(Liquid.class);
        when(mockLiquid.getHBarValue()).thenReturn(0.033);
        when(mockLiquid.getVolume()).thenReturn(1000.0);

        OkCoin mockOkCoin = mock(OkCoin.class);
        when(mockOkCoin.getHBarValue()).thenReturn(0.033);
        when(mockOkCoin.getVolume()).thenReturn(1000.0);

        Binance mockBinance = mock(Binance.class);
        when(mockBinance.getHBarValue()).thenReturn(0.035);
        when(mockBinance.getVolume()).thenReturn(1000.0);

        List<Exchange> exchanges = new ArrayList<>();
        exchanges.add(mockBinance);
        exchanges.add(mockCoinbase);
        exchanges.add(mockBitrex);
        exchanges.add(mockLiquid);
        exchanges.add(mockOkCoin);

        return exchanges;
    }
}
