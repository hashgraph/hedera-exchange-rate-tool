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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import static com.hedera.exchange.exchanges.Exchange.OBJECT_MAPPER;

/**
 * This is an Abstract class the each Exchange [ Bitrex, Coinbase .. etc.. ] needs to extend.
 * This implements the method to load the exchange rate HABR-USD
 * using the respective endpoint mentioned in the config file.
 *
 * @author Anirudh, Cesar
 */
public final class CoinFactory {

	private static final Logger LOGGER = LogManager.getLogger(CoinFactory.class);

	private HttpURLConnection connection;

	public CoinFactory() {
	}

	public CoinFactory(final HttpURLConnection connection) {
		this.connection = connection;
	}

	/**
	 * This Method fetches the data from the exchanges using the URL mentioned and creates an Object of type Exchange
	 * @param endpoint URL to the exchange to fetch the HABR-USD rate
	 * @param type Exchange class type.. Ex- Bitrex, Coinbase..
	 * @param <T> Exchange class type.. Ex- Bitrex, Coinbase..
	 * @return Exchange class type.. Ex- Bitrex, Coinbase..
	 */
	public <T extends Exchange> T load(final String endpoint, final Class<T> type) {
		try {
			final HttpURLConnection con = getConnection(endpoint);
			con.addRequestProperty("User-Agent",
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36");
			final InputStream inputStream = con.getInputStream();
			var jsonData = new BufferedReader(new InputStreamReader(inputStream))
					.lines().collect(Collectors.joining("\n"));
			jsonData = normalizeJsonData(jsonData);
			LOGGER.debug(Exchange.EXCHANGE_FILTER, "Json data for " + type.getSimpleName() + " after normalization : " + jsonData);
			final T exchange = OBJECT_MAPPER.readValue(jsonData, type);
			exchange.setEndPoint(endpoint);
			con.disconnect();
			return exchange;
		} catch (final Exception exception) {
			LOGGER.warn(Exchange.EXCHANGE_FILTER, "exchange loading failed : {}", exception.getMessage());
			return null;
		}
	}

	/**
	 * Some exchanges return a json array object as root. We should ignore this.
	 * @param jsonData json string to normalize
	 */
	private String normalizeJsonData(String jsonData) {
		if (jsonData.charAt(0) == '[') {
			jsonData = jsonData.substring(1, jsonData.length()-1);
		}
		return jsonData;
	}

	/**
	 * Get the Https connection using the URL provided.
	 * @param endpoint Endpoint to the exchange
	 * @return HttpURLConnection object to the URL
	 * @throws IOException
	 */
	public HttpURLConnection getConnection(final String endpoint) throws IOException {
		if (this.connection != null) {
			return this.connection;
		}

		final URL url = new URL(endpoint);
		return (HttpURLConnection) url.openConnection();
	}
}
