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

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.util.Base64;
import com.hedera.exchange.exchanges.Binance;
import com.hedera.exchange.exchanges.Bitrex;
import com.hedera.exchange.exchanges.PayBito;
import com.hedera.exchange.exchanges.UpBit;
import com.hedera.exchange.exchanges.Liquid;
import com.hedera.exchange.exchanges.OkCoin;
import com.hedera.exchange.exchanges.Coinbase;
import com.hedera.exchange.exchanges.CoinFactory;
import com.hedera.exchange.exchanges.ExchangeCoin;
import com.hedera.exchange.exchanges.Exchange;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.proto.NodeAddress;
import com.hedera.hashgraph.sdk.proto.NodeAddressBook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class implements helper functions of ERT
 *  1. To get the decrypted environment variables set in AWS
 *  2. To calculate median of the exchange rates fetched
 *  3. To calculate running weights
 *
 * @author Anirudh, Cesar
 */
public final class ExchangeRateUtils {

	private static final Logger LOGGER = LogManager.getLogger(ExchangeRateUtils.class);
	private static final Map<String, Class<? extends ExchangeCoin>> EXCHANGES = new HashMap<>();
	private static final int MILLI_SECS_IN_ONE_HOUR = 3_600_000;
	private static final int MILLI_SECS_IN_ONE_SEC = 1_000;

	static {
		EXCHANGES.put("bitrex", Bitrex.class);
		EXCHANGES.put("liquid", Liquid.class);
		EXCHANGES.put("coinbase", Coinbase.class);
		EXCHANGES.put("upbit", UpBit.class);
		EXCHANGES.put("okcoin", OkCoin.class);
		EXCHANGES.put("binance", Binance.class);
		EXCHANGES.put("paybito", PayBito.class);
	}

	private ExchangeRateUtils() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Get the decrypted Environment variable set in AWS
	 * for example: the DB endpoint, username, password to access the Database, config file path etc..
	 * @param environmentVariable - Encrypted variable
	 * @return decrypted Environment Variable.
	 */
	public static String getDecryptedEnvironmentVariableFromAWS(final String environmentVariable) {
		final String environmentValue = System.getenv(environmentVariable);
		final String lambdaFunctionName = System.getenv("AWS_LAMBDA_FUNCTION_NAME");
		return getDecryptedValueFromAWS(environmentValue, lambdaFunctionName);
	}

	static String getDecryptedValueFromAWS(final String value, final String lambdaFunctionName) {
		Map<String, String> encryptionContext = new HashMap<>();
		encryptionContext.put("LambdaFunctionName", lambdaFunctionName);
		final byte[] encryptedKey = Base64.decode(value);

		final AWSKMS client = AWSKMSClientBuilder.defaultClient();

		final DecryptRequest request = new DecryptRequest()
				.withCiphertextBlob(ByteBuffer.wrap(encryptedKey))
				.withEncryptionContext(encryptionContext);

		final ByteBuffer plainTextKey = client.decrypt(request).getPlaintext();
		return new String(plainTextKey.array(), StandardCharsets.UTF_8);
	}

	/**
	 * Loads the list of Exchange objects with HBAR-USD exchange rate using the URL endpoints provided for each
	 * Exchange int he config file.
	 * @return List of Exchange objects.
	 */
	public static List<Exchange> generateExchanges(final Map<String, String> exchangeAPIs) {
		List<Exchange> exchanges = new ArrayList<>();
		final CoinFactory factory = new CoinFactory();

		for (final Map.Entry<String, String> api : exchangeAPIs.entrySet()) {

			final Class<? extends ExchangeCoin> exchangeClass = EXCHANGES.get(api.getKey());

			final String endpoint = api.getValue();
			final Exchange actualExchange = factory.load(endpoint, exchangeClass);
			if (actualExchange == null) {
				LOGGER.error(Exchange.EXCHANGE_FILTER,"API {} not loaded for type {}", api.getKey(), exchangeClass);
				continue;
			}

			exchanges.add(actualExchange);
		}

		return exchanges;
	}

	/**
	 * This method parses the address book and generates a map of nodeIds and their Addresses.
	 * @param addressBook
	 * @return Map<String, String> nodeId --> IPaddress
	 */
	public static Map<String, String> getNodesFromAddressBook(final NodeAddressBook addressBook) {
		Map<String, String> nodes =  new HashMap<>();
		for(NodeAddress address : addressBook.getNodeAddressList()){
			String nodeId = address.getMemo().toStringUtf8();
			String nodeAddress = address.getIpAddress().toStringUtf8();
			if(!nodes.containsKey(nodeId)) {
				nodes.put(nodeId, nodeAddress + ":50211");
			}
			LOGGER.debug(Exchange.EXCHANGE_FILTER, "found node {} and its address {}:50211 in addressBook",
					nodeId, nodeAddress);
		}
		return  nodes;
	}

	/**
	 * Converts the string - string mapping of node id and address in the addressbook to
	 * AccountID - string map so that, it can be used in hedera client directly.
	 * @return
	 */
	public static Map<String, AccountId> getNodesForClient(final Map<String, String> nodes) {
		final Map<String, AccountId> accountToNodeAddresses = new HashMap<>();
		for (final Map.Entry<String, String> node : nodes.entrySet()) {
			final AccountId nodeId = AccountId.fromString(node.getKey());
			accountToNodeAddresses.put(node.getValue(), nodeId);
		}
		return accountToNodeAddresses;
	}

	/**
	 * Get the EPOC time of the end of the current hour in seconds in UTC.
	 * for example, say the current date and time is October 31st 2019, 10:34 AM
	 * then currentExpirationTime will be 1572537600 in UTC
	 * @return
	 */
	public static long getCurrentExpirationTime() {
		final long currentTime = System.currentTimeMillis();
		final long currentHourOnTheDot = currentTime - (currentTime % MILLI_SECS_IN_ONE_HOUR);
		final long currentExpirationTime = currentHourOnTheDot + MILLI_SECS_IN_ONE_HOUR;
		return currentExpirationTime / MILLI_SECS_IN_ONE_SEC;
	}

	/**
	 * Return the weighted median of the given values, using the given weights.
	 *
	 * The algorithm is equivalent to the following. Draw a bar chart, where bar
	 * number i has height value[i] and width weight[i]. At the top edge of each
	 * bar, draw a dot in the middle. Connect the dots with straight lines. Find
	 * the middle of the X axis: the height of the curve above that point is the
	 * weighted median.
	 *
	 * This differs from the algorithm by Edgeworth in 1888. That algorithm simply
	 * returns the height of the bar that is above the midpoint. That is fine if
	 * there are many data points. But it can be bad if there are very few bars,
	 * and they differ greatly in height. The algorithm used here returns a
	 * weighted average of that bar's height and it's neighbor's height, which is
	 * often a better fit to the intuitive notion of a good "representative value".
	 *
	 * @param values
	 *      the values for which the median should be found. These must be sorted ascending.
	 * @param weights
	 *      the positive weight for each value, with higher having more influence
	 * @return the weighted median
	 */
	public static double findVolumeWeightedMedianAverage(final double[] values, final double[] weights) throws IOException {
		int numberOfElements = values.length;
		double weightOfValueJustBelowMiddle;
		double weightOfValueJustAboveMiddle;
		double weightedAverage;
		double totalWeight = 0;
		double currentWeightSum;
		double nextWeightSum;
		double valueJustBelowMiddle;
		double valueJustAboveMiddle;

		for (int i = 0; i < numberOfElements; i++) {
			totalWeight += weights[i];
		}
		final double targetWeight = totalWeight / 2.0;
		currentWeightSum = weights[0] / 2;

		for (int index = 0; index < numberOfElements; index++) {
			nextWeightSum = currentWeightSum + (weights[index] + (index + 1 >= numberOfElements ? 0 : weights[index + 1])) / 2.0;
			if (nextWeightSum > targetWeight) {
				valueJustBelowMiddle = values[index];
				valueJustAboveMiddle = index + 1 >= numberOfElements ? 0 : values[index + 1];
				weightOfValueJustBelowMiddle = nextWeightSum - targetWeight;
				weightOfValueJustAboveMiddle = targetWeight - currentWeightSum;
				weightedAverage = (valueJustBelowMiddle * weightOfValueJustBelowMiddle +
						valueJustAboveMiddle * weightOfValueJustAboveMiddle) /
						(weightOfValueJustBelowMiddle + weightOfValueJustAboveMiddle);
				return weightedAverage;
			}
			currentWeightSum = nextWeightSum;
		}

		LOGGER.error(Exchange.EXCHANGE_FILTER, "This should never Happen. Given values are : \n Rates = " +
				Arrays.toString(values) + "\n Volumes = " + Arrays.toString(weights));
		throw new IOException("Couldn't find weighted median with the given values");
	}



	/**
	 * This Method takes in `activeRate` from receipt and calculated exchangeRate `goalRate`
	 * and calculate a weighted median with 90% of weight to `goalRate` and 10% to `activeRate`
	 * @param activeRate
	 * @param goalRate
	 * @return
	 * @throws Exception
	 */
	public static ExchangeRate calculateNewExchangeRate(
			final Rate activeRate,
			final ExchangeRate goalRate) throws IOException {

		double[] exchangeNextRates = new double[]{
				goalRate.getNextRate().getCentEquiv() / goalRate.getNextRate().getHBarEquiv(),
				activeRate.getCentEquiv() / activeRate.getHBarEquiv()
		};
		double[] exchangeCurrRates = new double[]{
				goalRate.getCurrentRate().getCentEquiv() / goalRate.getNextRate().getHBarEquiv(),
				activeRate.getCentEquiv() / activeRate.getHBarEquiv()
		};
		double[] exchangeVolumes = new double[] {9000, 1000};

		double newNextCenEquv = findVolumeWeightedMedianAverage(
				exchangeNextRates, exchangeVolumes);
		double newCurrCenEquv = findVolumeWeightedMedianAverage(
				exchangeCurrRates, exchangeVolumes);

		Rate newNextRate = new Rate(
				(int) goalRate.getNextRate().getHBarEquiv(),
				newNextCenEquv,
				goalRate.getNextRate().getExpirationTimeInSeconds());

		Rate newCurrRate = new Rate(
				(int) goalRate.getCurrentRate().getHBarEquiv(),
				newCurrCenEquv,
				goalRate.getCurrentRate().getExpirationTimeInSeconds());

		return new ExchangeRate(newCurrRate, newNextRate);
	}
}
