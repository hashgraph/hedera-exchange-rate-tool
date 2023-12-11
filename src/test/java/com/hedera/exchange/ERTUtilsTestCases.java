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
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.proto.NodeAddressBook;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ERTUtilsTestCases {

	private Map<String, String> nodes =  new HashMap<>();
	private NodeAddressBook addressBook;

	@Test
	void verifyNodesFromAddressBook() throws IOException {
		setupAddressBook();

		final Map<String, String> nodesFromAddressBook = ERTUtils.getNodesFromAddressBook(addressBook);
		for( String node : nodesFromAddressBook.keySet()){
			assertEquals(nodesFromAddressBook.get(node), nodes.get(node),
					"Nodes from AddressBook are not loaded correctly");
		}
	}

	@Test
	void getNodesForClientTest() throws IOException {
		//setup
		setupAddressBook();

		//when
		final Map<String, AccountId> nodesForClient = ERTUtils.getNodesForClient(nodes);

		//then
		assertEquals(26, nodesForClient.size(), "Not all Clients nodes are extracted");
		assertEquals(3, nodesForClient.get("35.237.200.180:50211").num,
				"Not the Ip address for node 3");
		assertEquals(7, nodesForClient.get("35.203.82.240:50211").num,
				"Not the Ip address for node 7");
		assertEquals(11, nodesForClient.get("35.240.118.96:50211").num,
				"Not the Ip address for node 11");
		assertEquals(15, nodesForClient.get("35.228.11.53:50211").num,
				"Not the Ip address for node 15");
	}

	@Test
	@Disabled
	/* This test doesnt work in CI. Needs an AWS SDK client to run.*/
	void getDecryptedEnvironmentVariableFromAWSTest() {
		final String expectedValue = "https://s3.amazonaws.com/exchange.rate.config.integration/config.json";
		final String encryptedValue = "AQICAHi3BYYdRzjj1ZR5ij/3mN6+GWqEbw7NTAG0fm7nzYo3MwHyBlKsmA+1lepLUe" +
				"+0rgeFAAAApzCBpAYJKoZIhvcNAQcGoIGWMIGTAgEAMIGNBgkqhkiG9w0BBwEwHgYJYIZIAWUDBAEuMBEEDLHJjkIANloMVIhCdgIBEIBg9HnXBKnxE3c4H5/17ilQR0G6DqZKH6dzBnhkUAjYbg1sBuStjVA8rQwBUtiSKO7b5ehQh+OxnrJxVbHAZNylSH71fr7OICMI3iA2qkIM8gtWNG1htphGhkDLCRcaw5Xh";
		final String lambdaFunctionName = "exchange-rate-tool-lambda-integration";
		assertEquals(expectedValue, ERTUtils.getDecryptedValueFromAWS(encryptedValue, lambdaFunctionName),
				"AWS lambda Decryption not working as expected.");
	}

	@Test
	void generateExchangesTest() {
		//setup
		final Map<String, String> exchangeAPIs = new HashMap<>() {{
			put("bitmart", "https://api-cloud.bitmart.com/contract/v1/tickers?contract_symbol=HBARUSDT");
			put("coinbase", "https://api.pro.coinbase.com/products/HBAR-USD/stats");
		}};

		//when
		final List<Exchange> exchanges = ERTUtils.generateExchanges(exchangeAPIs);

		//then
		assertEquals(2, exchanges.size(), "Couldn't generate all exchanges");
		assertNotNull(exchanges.get(0).getHBarValue(), "Exchange not generated correctly");
		assertNotNull(exchanges.get(1).getHBarValue(), "Exchange not generated correctly");
	}

	@Test
	void calculateNewExchangeRateTest() throws IOException {
		//given
		final long activeRateHbarEquiv = 30_000L;
		final long activeRateCentEquiv = 960_000L;
		final long expiry = 123_456_789L;
		final long currentRate = 450_000L;
		final long nextRate = 510_000L;
		final Rate activeRate = new Rate(activeRateHbarEquiv ,activeRateCentEquiv, expiry);
		final long expectedClippedCurrentRate = 501_000L;
		final long expectedClippedNextRate = 555_000L;
		final long newExpectedClippedCurrentRate = 528_000L;
		final long newExpectedClippedNextRate = 582_000L;
		final ExchangeRate exchangeRate = new ExchangeRate(
				new Rate(activeRateHbarEquiv ,currentRate, expiry),
				new Rate(activeRateHbarEquiv ,nextRate, expiry));

		//when
		ExchangeRate newExchangeRate = ERTUtils.calculateNewExchangeRate(activeRate, exchangeRate);

		//then
		assertEquals(expectedClippedCurrentRate, newExchangeRate.getCurrentRate().getCentEquiv(),
				"Weighted mean is not calculated as expected");
		assertEquals(expectedClippedNextRate, newExchangeRate.getNextRate().getCentEquiv(),
				"Weighted mean is not calculated as expected");

		newExchangeRate = ERTUtils.calculateNewExchangeRate(activeRate, newExchangeRate);

		assertEquals(newExpectedClippedCurrentRate, newExchangeRate.getCurrentRate().getCentEquiv(),
				"Weighted mean is not calculated as expected");
		assertEquals(newExpectedClippedNextRate, newExchangeRate.getNextRate().getCentEquiv(),
				"Weighted mean is not calculated as expected");
	}

	private void setupAddressBook() throws IOException {
		final File addressBookFile = new File("src/test/resources/addressBook.bin");
		final FileInputStream fis = new FileInputStream(addressBookFile);
		final byte[] content = new byte[(int) addressBookFile.length()];
		fis.read(content);
		addressBook = NodeAddressBook.parseFrom(content);

		nodes.put("0.0.3", "35.237.200.180:50211");
		nodes.put("0.0.4", "35.186.191.247:50211");
		nodes.put("0.0.5", "35.192.2.25:50211");
		nodes.put("0.0.6", "35.199.161.108:50211");
		nodes.put("0.0.7", "35.203.82.240:50211");
		nodes.put("0.0.8", "35.236.5.219:50211");
		nodes.put("0.0.9", "35.197.192.225:50211");
		nodes.put("0.0.10", "35.242.233.154:50211");
		nodes.put("0.0.11", "35.240.118.96:50211");
		nodes.put("0.0.12", "35.204.86.32:50211");
		nodes.put("0.0.13", "35.234.132.107:50211");
		nodes.put("0.0.14", "35.236.2.27:50211");
		nodes.put("0.0.15", "35.228.11.53:50211");
		nodes.put("0.0.16", "34.91.181.183:50211");
		nodes.put("0.0.17", "34.86.212.247:50211");
		nodes.put("0.0.18", "172.105.247.67:50211");
		nodes.put("0.0.19", "34.89.87.138:50211");
		nodes.put("0.0.20", "34.82.78.255:50211");
		nodes.put("0.0.21", "34.76.140.109:50211");
		nodes.put("0.0.22", "34.64.141.166:50211");
		nodes.put("0.0.23", "35.232.244.145:50211");
		nodes.put("0.0.24", "34.89.103.38:50211");
		nodes.put("0.0.25", "34.93.112.7:50211");
		nodes.put("0.0.26", "34.87.150.174:50211");
		nodes.put("0.0.27", "34.125.200.96:50211");
		nodes.put("0.0.28", "35.198.220.75:50211");
	}
}
