package com.hedera.services.exchange.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hedera.services.exchange.ExchangeRate;

public interface ExchangeDB {

	ExchangeRate getLastestExchangeRate();

	ExchangeRate getLatestMidnightExchangeRate();

	void pushExchangeRate(final ExchangeRate exchangeRate) throws JsonProcessingException, InterruptedException;
}
