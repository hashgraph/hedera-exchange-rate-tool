package com.hedera.services.exchange.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hedera.services.exchange.ExchangeRate;
import com.hedera.services.exchange.exchanges.Exchange;

import java.sql.SQLException;

public interface ExchangeDB {

	ExchangeRate getLatestExchangeRate() throws SQLException, Exception;

	ExchangeRate getLatestMidnightExchangeRate();

	void pushExchangeRate(final ExchangeRate exchangeRate) throws Exception;

	void pushMidnightRate(final ExchangeRate exchangeRate) throws Exception;

	void pushQueriedRate(final long expirationTime, final String queriedRate) throws Exception;
}
