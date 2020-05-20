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

import com.hedera.exchange.database.GCPExchangeRateDB;
import org.junit.jupiter.api.Test;

public class GCPTestCases {

    @Test
    public void addExchangeRate(){
       /* Rate currRate = new Rate(12,1566928800);
        Rate nextRate = new Rate(15,1566932400);

        ExchangeRate exchangeRate = new ExchangeRate(currRate, nextRate);
        GCPExcahgneRateDB.pushExchangeRate(exchangeRate);

        */
       GCPExchangeRateDB.main();

    }
}
