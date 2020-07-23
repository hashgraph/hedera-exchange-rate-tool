package com.hedera.exchange.database;

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

	ERTAddressBook getLatestERTAddressBook(String networkName) throws Exception;

	void pushERTAddressBook(long expirationTime, final ERTAddressBook ertAddressBook, String networkName) throws Exception;
}
