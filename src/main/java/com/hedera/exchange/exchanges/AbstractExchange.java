package com.hedera.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This is an Abstract class the each Exchange [ Bitrex, Coinbase .. etc.. ] needs to extend.
 * This implements the method to load the exchange rate HABR-USD
 * using the respective endpoint mentioned in the config file.
 *
 * @author Anirudh, Cesar
 */
public abstract class AbstractExchange implements Exchange {

	private static final Logger LOGGER = LogManager.getLogger(AbstractExchange.class);

	@JsonProperty("Query")
	String endPoint = "";

	/**
	 * This Method fetches the data from the exchanges using the URL mentioned and creates an Object of type Exchange
	 * @param endpoint URL to the exchange to fetch the HABR-USD rate
	 * @param type Exchange class type.. Ex- Bitrex, Coinbase..
	 * @param <T> Exchange class type.. Ex- Bitrex, Coinbase..
	 * @return Exchange class type.. Ex- Bitrex, Coinbase..
	 */
	public static <T extends Exchange> T load(final String endpoint, final Class<T> type) {
		try {
			final URL url = new URL(endpoint);
			final HttpURLConnection con = getConnection(url);
			con.addRequestProperty("User-Agent",
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36");
			final T exchange =  OBJECT_MAPPER.readValue(con.getInputStream(), type);
			exchange.setEndPoint(endpoint);
			con.disconnect();
			return exchange;
		} catch (final Exception exception) {
			LOGGER.debug(Exchange.EXCHANGE_FILTER, "exchange loading failed : {}", exception.getMessage());
			return null;
		}
	}

	@Override
	/**
	 * Converts the Exchange into a Json String using OBJECT_MAPPER
	 */
	public String toJson() throws JsonProcessingException {
		return OBJECT_MAPPER.writeValueAsString(this);
	}

	/**
	 * Set the url for the Exchange that we use to get the HABR-USD exchange rate
	 * @param url Exchange Endpoint
	 */
	public void setEndPoint(String url) {
		endPoint = url;
	}

	/**
	 * Get the Https connection using the URL provided.
	 * @param url URL to the exchange
	 * @return HttpURLConnection object to the URL
	 * @throws IOException
	 */
	protected static HttpURLConnection getConnection(final URL url) throws IOException {
		return (HttpURLConnection) url.openConnection();
	}
}
