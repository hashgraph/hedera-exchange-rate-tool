package com.hedera.services.exchange.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hedera.services.exchange.ExchangeRate;

import java.sql.SQLException;

public interface ExchangeDB {

	ExchangeRate getLatestExchangeRate() throws SQLException, Exception;

	ExchangeRate getLatestMidnightExchangeRate();

	void pushExchangeRate(final ExchangeRate exchangeRate) throws Exception;
}
