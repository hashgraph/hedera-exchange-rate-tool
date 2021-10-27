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
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 *
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * * Neither the name of JSR-310 nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.hedera.exchange.exchanges.Exchange;
import com.hedera.exchange.exchanges.Bitrex;
import com.hedera.exchange.exchanges.Binance;
import com.hedera.exchange.exchanges.Coinbase;
import com.hedera.exchange.exchanges.Liquid;
import com.hedera.exchange.exchanges.OkCoin;
import com.hedera.exchange.exchanges.PayBito;
import com.hedera.hashgraph.sdk.proto.ExchangeRateSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ERTProcessLogicTestCases {
    private String testARN = "arn:aws:sns:us-east-2:525755363515:ERT-PreProd";

    MockedStatic<ExchangeRateUtils> mockedExchangeRateUtils;
    MockedStatic<ERTNotificationHelper> mockedNotificationHelper;

    @BeforeEach
    void setUp() {
        mockedExchangeRateUtils = Mockito.mockStatic(ExchangeRateUtils.class);
        mockedExchangeRateUtils.when(
                () -> ExchangeRateUtils.getDecryptedEnvironmentVariableFromAWS(any())).thenReturn(testARN);
        mockedExchangeRateUtils.when(
                () -> ExchangeRateUtils.findVolumeWeightedMedianAverage(any(), any())).thenCallRealMethod();
        mockedNotificationHelper = Mockito.mockStatic(ERTNotificationHelper.class);
    }

    @AfterEach
    void cleanUp() {
        mockedExchangeRateUtils.close();
        mockedNotificationHelper.close();
    }

    @ParameterizedTest
    @CsvSource({"src/test/resources/configs/config.json,360000,288000",
                "src/test/resources/configs/config1.json,252000000,201600000",
                "src/test/resources/configs/config2.json,25920000,20736000"})
    public void testMedianWithDefaultAsMidnight(final String configPath, final int currentCentEquiv, final int expectedCentEquiv) throws Exception {
        List<Exchange> exchanges = this.setExchanges();

        final ERTParams params = ERTParams.readConfig(configPath);
        final ERTProcessLogic ertProcess = new ERTProcessLogic(params.getDefaultHbarEquiv(),
                exchanges,
                params.getBound(),
                params.getFloor(),
                new ExchangeRate(params.getDefaultRate(), params.getDefaultRate()),
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
        final ERTProcessLogic ertProcess = new ERTProcessLogic(params.getDefaultHbarEquiv(),
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
        final long currentExpirationInSeconds = ExchangeRateUtils.getCurrentExpirationTime();
        final Rate currentRate = new Rate(currentHBarEquiv, currentCentEquiv, currentExpirationInSeconds);
        final Rate expectedRate = new Rate(expectedHBarEquiv, expectedCentEquiv, currentExpirationInSeconds + 3_600);

        final ERTParams params = ERTParams.readConfig(configPath);
        final ERTProcessLogic ertProcess = new ERTProcessLogic(params.getDefaultHbarEquiv(),
                justBitrexExchange,
                params.getBound(),
                params.getFloor(),
                new ExchangeRate(currentRate, currentRate),
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
    public void testFloor(String configPath, long currentCentEquiv, long expectedCentEquiv) throws IOException {
        List<Exchange> exchanges = this.setFloorExchanges();
        final ERTParams params = ERTParams.readConfig(configPath);
        final Rate currentRate = new Rate(30000, currentCentEquiv,ExchangeRateUtils.getCurrentExpirationTime());
        final Rate midnightRate = new Rate(30000, currentCentEquiv,ExchangeRateUtils.getCurrentExpirationTime());

        final ERTProcessLogic ertProcess = new ERTProcessLogic(params.getDefaultHbarEquiv(),
                exchanges,
                params.getBound(),
                params.getFloor(),
                new ExchangeRate(midnightRate,midnightRate),
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
        final ERTProcessLogic ertProcess = new ERTProcessLogic(params.getDefaultHbarEquiv(),
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


    private List<Exchange> setOnlyBitrex(final double value) {

        Bitrex mockBitrex = mock(Bitrex.class);
        when(mockBitrex.getHBarValue()).thenReturn(value);
        when(mockBitrex.getVolume()).thenReturn(1000.0);

        List<Exchange> exchanges = new ArrayList<>();
        exchanges.add(mockBitrex);

        return exchanges;
    }

    private List<Exchange> setExchanges() {

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

        PayBito mockPayBito = mock(PayBito.class);
        when(mockPayBito.getHBarValue()).thenReturn(0.095);
        when(mockPayBito.getVolume()).thenReturn(1000.0);

        List<Exchange> exchanges = new ArrayList<>();
        exchanges.add(mockBinance);
        exchanges.add(mockCoinbase);
        exchanges.add(mockBitrex);
        exchanges.add(mockLiquid);
        exchanges.add(mockOkCoin);
        exchanges.add(mockPayBito);

        return exchanges;

    }

    private List<Exchange> setFloorExchanges() {

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

        PayBito mockPayBito = mock(PayBito.class);
        when(mockPayBito.getHBarValue()).thenReturn(0.035);
        when(mockPayBito.getVolume()).thenReturn(1000.0);

        List<Exchange> exchanges = new ArrayList<>();
        exchanges.add(mockBinance);
        exchanges.add(mockCoinbase);
        exchanges.add(mockBitrex);
        exchanges.add(mockLiquid);
        exchanges.add(mockOkCoin);
        exchanges.add(mockPayBito);

        return exchanges;
    }
}
