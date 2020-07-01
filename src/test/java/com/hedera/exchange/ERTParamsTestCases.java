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

import com.hedera.hashgraph.sdk.account.AccountId;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ERTParamsTestCases {

    @Test
    public void readCofiguration() throws Exception {
        final ERTParams ertParams = ERTParams.readConfig("src/test/resources/configs/config.json");
        assertEquals(25, ertParams.getBound());
        assertEquals("0.0.57", ertParams.getPayAccount());
        assertEquals("0.0.112", ertParams.getFileId());
        assertEquals(15_000, ertParams.getValidationDelayInMilliseconds());

        Map<String, Map<AccountId, String>> networks = ertParams.getNetworks();
        for(String network : networks.keySet()) {
            System.out.println("Network name is " + network);
            for(Map.Entry<AccountId, String> node : networks.get(network).entrySet()) {
                System.out.println("Node ID : " + node.getKey().toString());
                System.out.println("Node Address : " + node.getValue());
            }
        }
    }

    @Test
    public void readCofigurationFail() throws Exception {
        final ERTParams ertParams = ERTParams.readConfig("src/test/resources/configs/config1.json");
        assertEquals(25, ertParams.getBound());
        assertEquals("0.0.57", ertParams.getPayAccount());
        assertEquals(null, ertParams.getFileId());
        assertEquals(15_000, ertParams.getValidationDelayInMilliseconds());
    }

}
