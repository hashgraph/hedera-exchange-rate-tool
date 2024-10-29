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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BitMartTestCases {
    @Test
    public void fetchBitMartTest() throws IOException {
        final String urlString = "https://api-cloud-v2.bitmart.com/contract/public/details?symbol=HBARUSDT";
        final String result = "{\"code\":1000,\"message\":\"Ok\",\"data\":{\"symbols\":[{\"symbol\":\"HBARUSDT\",\"product_type\":1,\"open_timestamp\":1646006400000,\"expire_timestamp\":0,\"settle_timestamp\":0,\"base_currency\":\"HBAR\",\"quote_currency\":\"USDT\",\"last_price\":\"0.05038\",\"volume_24h\":\"146012288\",\"turnover_24h\":\"7179493.63916\",\"index_price\":\"0.05034125\",\"index_name\":\"HBARUSDT\",\"contract_size\":\"1\",\"min_leverage\":\"1\",\"max_leverage\":\"20\",\"price_precision\":\"0.00001\",\"vol_precision\":\"1\",\"max_volume\":\"5000000\",\"min_volume\":\"1\",\"funding_rate\":\"0.0001\",\"expected_funding_rate\":\"0.0001\",\"open_interest\":\"2934234\",\"open_interest_value\":\"152260.12285\",\"high_24h\":\"0.0506\",\"low_24h\":\"0.04734\",\"change_24h\":\"0.0575146935348447\",\"funding_time\":1730246400000,\"market_max_volume\":\"0\",\"funding_interval_hours\":8}]},\"trace\":\"f32079c6924b4c2b9d8d90b038917409.68.17302227080767594\"}";
        final InputStream json = new ByteArrayInputStream(result.getBytes());
        final HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenReturn(json);

        final CoinFactory factory = new CoinFactory(connection);

        final BitMart bitmart = factory.load(urlString, BitMart.class);

        assertNotNull(bitmart);
        assertEquals("Ok", bitmart.getMessage());
        assertEquals(0.05038, bitmart.getHBarValue());
        assertEquals(146012288, bitmart.getVolume());
    }
}
