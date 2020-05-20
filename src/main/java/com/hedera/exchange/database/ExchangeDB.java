package com.hedera.exchange.database;

import com.hedera.exchange.ERTAddressBook;
import com.hedera.exchange.ExchangeRate;

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

	ERTAddressBook getLatestERTAddressBook() throws Exception;

	void pushERTAddressBook(long expirationTime, final ERTAddressBook ertAddressBook) throws Exception;
}
