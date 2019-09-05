package com.hedera.services.exchange.database;

import com.hedera.services.exchange.ExchangeRate;
import com.hedera.services.exchange.exchanges.Exchange;

public interface ExchangeDB {

	ExchangeRate getLatestExchangeRate() throws Exception;

	ExchangeRate getLatestMidnightExchangeRate() throws Exception;

	void pushExchangeRate(final ExchangeRate exchangeRate) throws Exception;

	void pushMidnightRate(final ExchangeRate exchangeRate) throws Exception;

	void pushQueriedRate(final long expirationTime, final String queriedRate) throws Exception;

}
