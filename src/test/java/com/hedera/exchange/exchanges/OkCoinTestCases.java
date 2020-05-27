package com.hedera.exchange.exchanges;

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
        final String result = "{\"product_id\":\"HBAR-USD\", \"instrument_id\":\"HBAR-USD\", \"last\": 0.008754}";
        final InputStream json = new ByteArrayInputStream(result.getBytes());
        final HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenReturn(json);
        new MockUp<Liquid>() {
            @Mock
            HttpURLConnection getConnection(final URL url) {
                return connection;
            }
        };

        OkCoin okcoin = new OkCoin();
        okcoin = okcoin.load("https://www.okcoin.com/api/spot/v3/instruments/HBAR-USD/ticker");
        assertEquals(0.008754, okcoin.getHBarValue());
        assertEquals("HBAR-USD", okcoin.getInstrumentid());
        assertEquals("HBAR-USD", okcoin.getProductid());
    }

    @Test
    public void fetchOkCoinWithNullResultTest() throws IOException {
        final String result = "{\"product_id\":null, \"instrument_id\":\"HBAR-USD\", \"last\": null}";
        final InputStream json = new ByteArrayInputStream(result.getBytes());
        final HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenReturn(json);
        new MockUp<Bitrex>() {
            @Mock
            HttpURLConnection getConnection(final URL url) {
                return connection;
            }
        };

        OkCoin okcoin = new OkCoin();
        okcoin = okcoin.load("https://www.okcoin.com/api/spot/v3/instruments/HBAR-USD/ticker");
        assertEquals("HBAR-USD", okcoin.getInstrumentid());
        assertNull(okcoin.getHBarValue());
        assertNull(okcoin.getProductid());
    }
}
