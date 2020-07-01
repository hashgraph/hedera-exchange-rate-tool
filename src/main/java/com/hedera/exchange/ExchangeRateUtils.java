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
import com.hedera.exchange.exchanges.Binance;
import com.hedera.exchange.exchanges.Bitrex;
import com.hedera.exchange.exchanges.UpBit;
import com.hedera.exchange.exchanges.Liquid;
import com.hedera.exchange.exchanges.OkCoin;
import com.hedera.exchange.exchanges.Coinbase;
import com.hedera.exchange.exchanges.CoinFactory;
import com.hedera.exchange.exchanges.ExchangeCoin;
import com.hedera.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
public class ExchangeRateUtils {

	private static final Logger LOGGER = LogManager.getLogger(ExchangeRateUtils.class);
	private static final Map<String, Class<? extends ExchangeCoin>> EXCHANGES = new HashMap<>();

	static {
		EXCHANGES.put("bitrex", Bitrex.class);
		EXCHANGES.put("liquid", Liquid.class);
		EXCHANGES.put("coinbase", Coinbase.class);
		EXCHANGES.put("upbit", UpBit.class);
		EXCHANGES.put("okcoin", OkCoin.class);
		EXCHANGES.put("binance", Binance.class);
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

	public static String getDecryptedValueFromAWS(final String value, final String lambdaFunctionName) {
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
	public List<Exchange> generateExchanges( final Map<String, String> exchangeApis) {
		List<Exchange> exchanges = new ArrayList<>();
		final CoinFactory factory = new CoinFactory();

		for (final Map.Entry<String, String> api : exchangeApis.entrySet()) {

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
}
