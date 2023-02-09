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
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 *
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * * Neither the name of JSR-310 nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.hedera.exchange.ERTAddressBook;
import com.hedera.exchange.Environment;
import com.hedera.exchange.ExchangeRate;
import com.hedera.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.hedera.exchange.ExchangeRateTool.env;
import static com.hedera.exchange.database.DBParams.getEndpoint;
import static com.hedera.exchange.database.DBParams.getGcpDataSource;
import static com.hedera.exchange.database.DBParams.getPassword;
import static com.hedera.exchange.database.DBParams.getUsername;

/**
 * This class implements the ExchangeDB interface
 * which species what APIs that we need to fetch/push data into the Database.
 *
 * If you foresee doing more of this mapping, I would recommend moving to JPA/Hibernate.
 *
 */
public class QueryHelper implements ExchangeDB {

	private static final Logger LOGGER = LogManager.getLogger(QueryHelper.class);

	private static final String LATEST_EXCHANGE_QUERY = "SELECT e1.expiration_time, e1.exchange_rate_file FROM exchange_rate AS e1 INNER JOIN (SELECT MAX(expiration_time) expiration_time FROM exchange_rate) AS e2 ON e1.expiration_time = e2.expiration_time LIMIT 1";

	private static final String LATEST_ADDRESS_BOOK_QUERY = "SELECT e1.expiration_time, e1.address_book FROM address_book AS e1 INNER JOIN (SELECT MAX(expiration_time) expiration_time FROM address_book) AS e2 ON e1.expiration_time = e2.expiration_time and network_name = ? LIMIT 1";

	private static final String MIDNIGHT_EXCHANGE_QUERY = "SELECT e1.expiration_time, e1.exchange_rate_file FROM midnight_rate AS e1 INNER JOIN (SELECT MAX(expiration_time) expiration_time FROM midnight_rate) AS e2 ON e1.expiration_time = e2.expiration_time LIMIT 1";

	private static final String LATEST_QUERIED_QUERY = "SELECT e1.expiration_time, e1.queried_rates FROM queried_rates AS e1 INNER JOIN (SELECT MAX(expiration_time) expiration_time FROM queried_rates) AS e2 ON e1.expiration_time = e2.expiration_time LIMIT 1";

	public QueryHelper() {
		this.migrate();
	}

	private void migrate() {
		final Flyway flyway;
		if (env == Environment.AWS) {
			flyway = Flyway.configure()
					.dataSource(getEndpoint(), getUsername(), getPassword())
					.baselineOnMigrate(true)
					.load();
		} else {
			flyway = Flyway.configure()
					.dataSource(getGcpDataSource())
					.baselineOnMigrate(true)
					.load();
		}
		flyway.migrate();
	}

	@Override
	public ExchangeRate getMidnightExchangeRate(long expirationTime) throws SQLException, IOException {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get the midnight exchange rate from midnight rate table " +
				"with nextExpiration time :{}", expirationTime);
		try (final Connection conn = DBParams.getConnection();
			 final PreparedStatement prepStatement = conn.prepareStatement(
					 "SELECT expiration_time, exchange_rate_file FROM midnight_rate where expiration_time = ?")){
			prepStatement.setLong(1, expirationTime);
			final ResultSet result = prepStatement.executeQuery();
			if (result.next()) {
				LOGGER.info(Exchange.EXCHANGE_FILTER, "the midnight rate at {} is {}", expirationTime, result.getString(2));
				return ExchangeRate.fromJson(result.getString(2));
			}
			LOGGER.warn(Exchange.EXCHANGE_FILTER, "failed to midnight exchange rate from midnight rate table " +
					"with expirationTime {}", expirationTime);
			return null;
		}
	}

	@Override
	public ERTAddressBook getLatestERTAddressBook(String networkName) throws SQLException, IOException {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get latest ERTAddressBook from address_book table");
		try (final Connection conn = DBParams.getConnection();
			 final PreparedStatement statement = conn.prepareStatement(LATEST_ADDRESS_BOOK_QUERY)) {
			statement.setString(1, networkName);
			LOGGER.info(Exchange.EXCHANGE_FILTER,"final query for addressbook : {}",
					statement.toString());
			final ResultSet result = statement.executeQuery();
			if (result.next()) {
				LOGGER.info(Exchange.EXCHANGE_FILTER, "the latest ERTAddressBook from address_book table {} for network {}",
						result.getString(2), networkName);
				return ERTAddressBook.fromJson(result.getString(2));
			}
			LOGGER.warn(Exchange.EXCHANGE_FILTER, "failed to get latest ERTAddressBook from address_book table ");
			return null;
		}
	}

	@Override
	public void pushERTAddressBook(long expirationTime, ERTAddressBook ertAddressBook, String networkName)
			throws SQLException, IOException {
		String addressBook = ertAddressBook.toJson();
		LOGGER.info(Exchange.EXCHANGE_FILTER, "push latest addressBook to  address_book table : {}",
				addressBook);
		try (final Connection conn = DBParams.getConnection();
			 final PreparedStatement statement = conn.prepareStatement(
					 "INSERT INTO address_book (expiration_time, address_book, network_name) VALUES(?,?::JSON,?)")) {
			statement.setLong(1, expirationTime);
			statement.setObject(2, addressBook);
			statement.setString(3,networkName);
			statement.executeUpdate();
		}
	}

	@Override
	public ExchangeRate getLatestExchangeRate() throws SQLException, IOException {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get latest exchange rate from exchange rate table");
		try (final Connection conn = DBParams.getConnection();
			 final Statement statement = conn.createStatement();
			 final ResultSet result = statement.executeQuery(LATEST_EXCHANGE_QUERY)) {
				if (result.next()) {
					LOGGER.info(Exchange.EXCHANGE_FILTER, "the latest exchange rate : {}", result.getString(2));
					return ExchangeRate.fromJson(result.getString(2));
				}
			LOGGER.warn(Exchange.EXCHANGE_FILTER, "failed to get latest exchange rate from exchange rate table ");
			return null;
		}
	}

	@Override
	public ExchangeRate getExchangeRate(long expirationTime) throws SQLException, IOException {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get the exchange rate from exchange rate table " +
				"with nextExpiration time :{}", expirationTime);
		try (final Connection conn = DBParams.getConnection();
			 final PreparedStatement prepStatement = conn.prepareStatement(
			 "SELECT expiration_time, exchange_rate_file FROM exchange_rate where expiration_time = ?")){
			prepStatement.setLong(1, expirationTime);
			final ResultSet result = prepStatement.executeQuery();
			if (result.next()) {
				LOGGER.info(Exchange.EXCHANGE_FILTER, "the exchange rate at {} is {}", expirationTime, result.getString(2));
				return ExchangeRate.fromJson(result.getString(2));
			}
			LOGGER.warn(Exchange.EXCHANGE_FILTER, "failed to exchange rate from exchange rate table " +
					"with expirationTime {}", expirationTime);
			return null;
		}
	}

	@Override
	public String getQueriedRate(long expirationTime) throws SQLException {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get the queried rate from queried rate table " +
				"with nextExpiration time :{}", expirationTime);
		try (final Connection conn = DBParams.getConnection();
			 final PreparedStatement prepStatement = conn.prepareStatement(
					 "SELECT expiration_time, queried_rates FROM queried_rates where expiration_time = ?")){
			prepStatement.setLong(1, expirationTime);
			final ResultSet result = prepStatement.executeQuery();
			if (result.next()) {
				LOGGER.info(Exchange.EXCHANGE_FILTER, "the queried rate at {} is {}", expirationTime, result.getString(2));
				return result.getString(2);
			}
			LOGGER.warn(Exchange.EXCHANGE_FILTER, "failed to get the queried rate from queried rate table " +
					"with expirationTime {}", expirationTime);
			return null;
		}
	}

	@Override
	public ExchangeRate getLatestMidnightExchangeRate() throws SQLException, IOException {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get midnight exchange rate from midnight rate table");
		try (final Connection conn = DBParams.getConnection();
			 final Statement statement = conn.createStatement();
			 final ResultSet result = statement.executeQuery(MIDNIGHT_EXCHANGE_QUERY)) {
			if (result.next()) {
				LOGGER.info(Exchange.EXCHANGE_FILTER, "the midnight exchange rate : {}", result.getString(2));
				return ExchangeRate.fromJson(result.getString(2));
			}
			LOGGER.warn(Exchange.EXCHANGE_FILTER, "failed to get latest exchange rate from midnight rate table ");
			return null;
		}
	}

	@Override
	public String getLatestQueriedRate() throws SQLException {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get the latest queried rate");
		try (final Connection conn = DBParams.getConnection();
			 final Statement statement = conn.createStatement();
			 final ResultSet result = statement.executeQuery(LATEST_QUERIED_QUERY)) {
			if (result.next()) {
				LOGGER.info(Exchange.EXCHANGE_FILTER, "the queried rate : {}", result.getString(2));
				return result.getString(2);
			}
			LOGGER.warn(Exchange.EXCHANGE_FILTER, "failed to get latest queried rate");
			return null;
		}
	}

	@Override
	public void pushExchangeRate(ExchangeRate exchangeRate) throws SQLException, IOException {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "push latest exchange rate to exchange rate table : {}",
				exchangeRate.toJson());
		try (final Connection conn = DBParams.getConnection();
			 final PreparedStatement statement = conn.prepareStatement(
					 "INSERT INTO exchange_rate (expiration_time, exchange_rate_file) VALUES(?,?::JSON)")) {
			statement.setLong(1, exchangeRate.getNextExpirationTimeInSeconds());
			statement.setObject(2, exchangeRate.toJson());
			statement.executeUpdate();
		}
	}

	@Override
	public void pushMidnightRate(ExchangeRate exchangeRate) throws SQLException, IOException {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "push the midnight exchange rate to midnight rate table : {}",
				exchangeRate.toJson());
		try (final Connection conn = DBParams.getConnection();
			 final PreparedStatement statement = conn.prepareStatement(
					 "INSERT INTO midnight_rate (expiration_time, exchange_rate_file) VALUES(?,?::JSON)")) {
			statement.setLong(1, exchangeRate.getNextExpirationTimeInSeconds());
			statement.setObject(2, exchangeRate.toJson());
			statement.executeUpdate();
		}
	}

	@Override
	public void pushQueriedRate(long expirationTime, String queriedRate) throws SQLException {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "push the queried exchanges to queried rate table : {}:{}",
				expirationTime, queriedRate);
		try (final Connection conn = DBParams.getConnection();
			 final PreparedStatement statement = conn.prepareStatement(
					 "INSERT INTO queried_rates (expiration_time, queried_rates) VALUES(?,?::JSON)")) {
			statement.setLong(1, expirationTime);
			statement.setObject(2, queriedRate);
			statement.executeUpdate();
		}
	}

}
