package com.hedera.services.exchange.database;

import com.hedera.services.exchange.ExchangeRate;
import com.hedera.services.exchange.exchanges.Exchange;

public interface ExchangeDB {

	ExchangeRate getLatestExchangeRate() throws Exception;

	ExchangeRate getExchangeRate(long expirationTime) throws  Exception;

	ExchangeRate getLatestMidnightExchangeRate() throws Exception;

	public String getQueriedRate(long expirationTime) throws Exception;

	public String getLatestQueriedRate() throws Exception;

	void pushExchangeRate(final ExchangeRate exchangeRate) throws Exception;

	void pushMidnightRate(final ExchangeRate exchangeRate) throws Exception;

	void pushQueriedRate(final long expirationTime, final String queriedRate) throws Exception;

	ExchangeRate getMidnightExchangeRate(long expirationTime) throws Exception;
}
