package com.hedera.exchange.server;

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
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.hedera.exchange.database.DBParams;
import com.hedera.exchange.database.ExchangeDB;
import com.hedera.exchange.database.ExchangeRatePostgresDB;
import com.hedera.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;

final class DatabaseConnection {

	private static final Logger LOGGER = LogManager.getLogger(DatabaseConnection.class);

	private DatabaseConnection() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Waits for the database instead of exiting, so a DB that starts after this pod doesn't cause crash loops.
	 * Connecting also applies any pending schema migrations. Only connection failures are retried; anything else
	 * (bad configuration, missing driver) fails fast.
	 */
	static ExchangeDB connectWithRetry() throws InterruptedException {
		while (true) {
			try {
				return new ExchangeRatePostgresDB(new DBParams());
			} catch (RuntimeException e) {
				if (!isConnectionFailure(e)) {
					throw e;
				}
				final String reason = String.valueOf(e.getMessage()).strip().lines().findFirst().orElse("");
				LOGGER.warn(Exchange.EXCHANGE_FILTER, "Database not reachable yet, retrying in 5s: {}", reason);
				Thread.sleep(5_000);
			}
		}
	}

	private static boolean isConnectionFailure(final Throwable e) {
		for (Throwable t = e; t != null; t = t.getCause()) {
			if (t instanceof SQLException) {
				return true;
			}
		}
		return false;
	}
}
