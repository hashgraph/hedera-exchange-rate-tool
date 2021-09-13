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

import com.hedera.exchange.database.ExchangeDB;
import com.hedera.exchange.database.ExchangeRateAWSRD;
import com.hedera.hashgraph.sdk.AccountId;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.hedera.exchange.ExchangeRateTool.DEFAULT_RETRIES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExchangeRateToolTestCases {

	ExchangeRateTool mockERT;
	String networkName;
	Map<String, Map<String, AccountId>> networks;
	AccountId operatorId;
	ExchangeRate exchangeRate;
	ExchangeRate midnightExchangeRate;
	ERTParams ertParams;
	ExchangeDB exchangeDB;
	ERTAddressBook ertAddressBookFromPreviousRun;

//	@Test
//	public void doesRetryTest() throws Exception {
//		//setup
//		setup();
//		doThrow(new Exception()).when(mockERT).fileUpdateTransactionForNetwork(
//				networkName, operatorId, exchangeRate, midnightExchangeRate, networks);
//
//		//when
//		mockERT.performFileUpdateOnNetwork(networkName,networks,operatorId,exchangeRate,midnightExchangeRate);
//
//		//assert
//		verify(mockERT, times(DEFAULT_RETRIES)).fileUpdateTransactionForNetwork(
//				networkName, operatorId, exchangeRate, midnightExchangeRate, networks
//		);
//	}

	@Test
	public void callsFileUpdateOnEachNetwork() throws Exception {
		//given
		mockSetup();

		//when
		mockERT.execute();

		//then
		verify(mockERT, times(1)).fileUpdateTransactionForNetwork(
				"localHost",
				AccountId.fromString("0.0.57"),
				exchangeRate,
				midnightExchangeRate,
				networks.get("localHost")
		);
	}

	private void mockSetup() throws IOException, SQLException {
		ertParams = mock(ERTParams.class);
		exchangeDB = mock(ExchangeRateAWSRD.class);

		try (MockedStatic<ERTParams> mockParams = Mockito.mockStatic(ERTParams.class)) {
			mockParams.when( () -> ERTParams.readConfig((String) any())).thenReturn(ertParams);
		}
		when(ertParams.getExchangeDB()).thenReturn(exchangeDB);

		mockERT = new ExchangeRateTool();
//		mockERT.setExchangeDB(exchangeDB);
//		mockERT.setErtParams(ertParams);

		networkName = "localHost";
		operatorId = AccountId.fromString("0.0.57");
		exchangeRate = new ExchangeRate(
				new Rate(1, 10, 123456),
				new Rate(1,11,128910)
		);
		midnightExchangeRate = new ExchangeRate(
				new Rate(1, 9, 123456),
				new Rate(1,10,128910)
		);
		networks = new HashMap<>() {{
			put("localHost", new HashMap<>() {{
				put("127.0.0.1:501211", AccountId.fromString("0.0.3"));
			}});
		}};
		Map<String, String> exchangeAPIs = new HashMap<>() {{
			put("bitrex", "https://api.bittrex.com/api/v1.1/public/getmarketsummary?market=USD-HBAR");
			put("okcoin", "https://www.okcoin.com/api/spot/v3/instruments/HBAR-USD/ticker");
			put("paybito", "https://trade.paybito.com/api/trades/HBAR_USD");
		}};

		when(ertParams.getNetworks()).thenReturn(networks);
		when(ertParams.getFrequencyInSeconds()).thenReturn(3600L);
		when(exchangeDB.getLatestMidnightExchangeRate()).thenReturn(midnightExchangeRate);
		when(exchangeDB.getLatestExchangeRate()).thenReturn(exchangeRate);
		when(ertParams.getOperatorId()).thenReturn("0.0.57");
		when(ertParams.getExchangeAPIList()).thenReturn(exchangeAPIs);

	}

}
