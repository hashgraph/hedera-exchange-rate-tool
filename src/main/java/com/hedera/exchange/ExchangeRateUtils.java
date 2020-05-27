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

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.util.Base64;
import com.hedera.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/**
 * This class implements helper functions of ERT
 *  1. To get the decrypted environment variables set in AWS
 *  2. To calculate median of the exchange rates fetched
 *  3. To calculate running weights
 *
 * @author Anirudh, Cesar
 */
public class ExchangeRateUtils {

	private static final Logger LOGGER = LogManager.getLogger(ExchangeRateUtils.class);

	/**
	 * Get the decrypted Environment variable set in AWS
	 * for example: the DB endpoint, username, password to access the Database, config file path etc..
	 * @param environmentVariable - Encrypted variable
	 * @return decrypted Environment Variable.
	 */
	public static String getDecryptedEnvironmentVariableFromAWS(final String environmentVariable) {
		final byte[] encryptedKey = Base64.decode(System.getenv(environmentVariable));

		final AWSKMS client = AWSKMSClientBuilder.defaultClient();

		final DecryptRequest request = new DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(encryptedKey));
		final ByteBuffer plainTextKey = client.decrypt(request).getPlaintext();
		return new String(plainTextKey.array(), StandardCharsets.UTF_8);
	}

	/**
	 * Calculates the Median among the exchange rates fetched from the exchanges
	 * @param exchanges - list of Exchange objects that have exchange rates of HABR-USD
	 * @return median of the exchange rates
	 */
	public static Double calculateMedianRate(List<Exchange> exchanges) {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "Computing median");

		LOGGER.info(Exchange.EXCHANGE_FILTER, "removing all invalid exchanges retrieved");
		exchanges.removeIf(x -> x == null ||
				x.getHBarValue() == null ||
				x.getHBarValue() == 0.0
		);

		if (exchanges.size() == 0){
			LOGGER.error(Exchange.EXCHANGE_FILTER, "No valid exchange rates retrieved.");
			return null;
		}

		exchanges.sort(Comparator.comparingDouble(Exchange::getHBarValue));
		LOGGER.info(Exchange.EXCHANGE_FILTER, "sorted the exchange rates... calculate the weighted median now");

		double[] exchangeRate = new double[exchanges.size()];
		double[] volumes = new double[exchanges.size()+1];
		double[] volumeSum = new double[exchanges.size()+1];

		volumes[exchanges.size()] = 0.0;
		volumeSum[0] = 0.0;

		for( int i = 0; i < exchanges.size(); i++ ) {
			exchangeRate[i] = exchanges.get(i).getHBarValue();
			volumes[i] = exchanges.get(i).getVolume();
			volumeSum[i+1] = volumeSum[i] + volumes[i];
		}

		int partitionIndex = findPartitionIndex(volumeSum);

		if( partitionIndex == 0 ) {
			return exchangeRate[partitionIndex];
		}

		double first = exchangeRate[partitionIndex];
		double second = (partitionIndex + 1) < exchangeRate.length ? exchangeRate[partitionIndex + 1] : 0.0;
		double third = (partitionIndex + 2) < exchangeRate.length ? exchangeRate[partitionIndex + 2] : 0.0;

		double left = volumeSum[partitionIndex];
		double mid = volumeSum[partitionIndex + 1];
		double right = volumeSum[ volumeSum.length - 1 ] - left - mid;

		double a = Math.max(0.0, left - right);
		double b = mid;
		double c = Math.max(0.0, right - left);

		return (a * first + b * second + c * third) / (a + b + c);

//		if (exchanges.size() % 2 == 0 ) {
//			return (exchanges.get(exchanges.size() / 2).getHBarValue() +
//					exchanges.get(exchanges.size() / 2 - 1).getHBarValue()) / 2;
//		}
//		else {
//			return exchanges.get(exchanges.size() / 2).getHBarValue();
//		}
	}

	private static int findPartitionIndex(double[] volumeSum) {
		int result = 0;
		double halfSum = volumeSum[volumeSum.length - 1] / 2.0;

		for( int i = 0; i < volumeSum.length; i++) {
			if( volumeSum[i] < halfSum ) {
				result = i;
			}
		}
		return result;
	}
}
