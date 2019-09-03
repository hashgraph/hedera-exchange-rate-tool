package com.hedera.services.exchange.database;

import com.hedera.services.exchange.ExchangeRate;

public interface ExchangeDB {

	ExchangeRate getLatestExchangeRate() throws Exception;

	ExchangeRate getLatestMidnightExchangeRate();

	void pushExchangeRate(final ExchangeRate exchangeRate) throws Exception;
}
