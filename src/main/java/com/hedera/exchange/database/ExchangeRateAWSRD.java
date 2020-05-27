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
import com.hedera.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class implements the ExchangeDB interface using AWS RDS
 * which species what APIs that we need to fetch/push data into the Database.
 *
 * If you foresee doing more of this mapping, I would recommend moving to JPA/Hibernate.
 *
 */
public class ExchangeRateAWSRD implements ExchangeDB {

	private static final Logger LOGGER = LogManager.getLogger(ExchangeRateAWSRD.class);

	private static final String LATEST_EXCHANGE_QUERY = "SELECT e1.expirationTime, e1.exchangeRateFile FROM exchange_rate AS e1 INNER JOIN (SELECT MAX(expirationTime) expirationTime FROM exchange_rate) AS e2 ON e1.expirationTime = e2.expirationTime LIMIT 1";

	private static final String LATEST_ADDRESSBOOK_QUERY = "SELECT e1.expirationTime, e1.addressBook FROM address_book AS e1 INNER JOIN (SELECT MAX(expirationTime) expirationTime FROM address_book) AS e2 ON e1.expirationTime = e2.expirationTime LIMIT 1";

	private static final String MIDNIGHT_EXCHANGE_QUERY = "SELECT e1.expirationTime, e1.exchangeRateFile FROM midnight_rate AS e1 INNER JOIN (SELECT MAX(expirationTime) expirationTime FROM midnight_rate) AS e2 ON e1.expirationTime = e2.expirationTime LIMIT 1";

	private static final String LATEST_QUERIED_QUERY = "SELECT e1.expirationTime, e1.queriedrates FROM queried_rate AS e1 INNER JOIN (SELECT MAX(expirationTime) expirationTime FROM queried_rate) AS e2 ON e1.expirationTime = e2.expirationTime LIMIT 1";

	private final AWSDBParams params;

	public ExchangeRateAWSRD(final AWSDBParams params) {
		this.params = params;
		this.migrate();
	}

	private void migrate() {
		final Flyway flyway = Flyway.configure()
				.dataSource(this.params.getEndpoint(),
						this.params.getUsername(),
						this.params.getPassword())
				.baselineOnMigrate(true)
				.load();
		flyway.migrate();
	}

	@Override
	public ExchangeRate getMidnightExchangeRate(long expirationTime) throws Exception {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get the midnight exchange rate from midnight rate table " +
				"with nextExpiration time :{}", expirationTime);
		try (final Connection conn = getConnection();
			 final PreparedStatement prepStatement = conn.prepareStatement(
					 "SELECT expirationTime, exchangeRateFile FROM midnight_rate where expirationTime = ?")){
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
	public ERTAddressBook getLatestERTAddressBook() throws Exception {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get latest ERTAddressBook from address_book table");
		try (final Connection conn = getConnection();
			 final Statement statement = conn.createStatement();
			 final ResultSet result = statement.executeQuery(LATEST_ADDRESSBOOK_QUERY)) {
			if (result.next()) {
				LOGGER.info(Exchange.EXCHANGE_FILTER, "the latest ERTAddressBook from address_book table {}", result.getString(2));
				return ERTAddressBook.fromJson(result.getString(2));
			}
			LOGGER.warn(Exchange.EXCHANGE_FILTER, "failed to get latest ERTAddressBook from address_book table ");
			return null;
		}
	}

	@Override
	public void pushERTAddressBook(long expirationTime, ERTAddressBook ertAddressBook) throws Exception {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "push latest addressBook to  address_book table : {}",
				ertAddressBook.toJson());
		try (final Connection conn = getConnection();
			 final PreparedStatement statement = conn.prepareStatement(
					 "INSERT INTO address_book (expirationTime,addressBook) VALUES(?,?::JSON)")) {
			statement.setLong(1, expirationTime);
			statement.setObject(2, ertAddressBook.toJson());
			statement.executeUpdate();
		}
	}

	@Override
	public ExchangeRate getLatestExchangeRate() throws Exception {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get latest exchange rate from exchange rate table");
		try (final Connection conn = getConnection();
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
	public ExchangeRate getExchangeRate(long expirationTime) throws Exception {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get the exchange rate from exchange rate table " +
				"with nextExpiration time :{}", expirationTime);
		try (final Connection conn = getConnection();
			 final PreparedStatement prepStatement = conn.prepareStatement(
			 "SELECT expirationTime, exchangeRateFile FROM exchange_rate where expirationTime = ?")){
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
	public String getQueriedRate(long expirationTime) throws Exception {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get the queried rate from queried rate table " +
				"with nextExpiration time :{}", expirationTime);
		try (final Connection conn = getConnection();
			 final PreparedStatement prepStatement = conn.prepareStatement(
					 "SELECT expirationTime, queriedrates FROM queried_rate where expirationTime = ?")){
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
	public ExchangeRate getLatestMidnightExchangeRate() throws Exception {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get midnight exchange rate from midnight rate table");
		try (final Connection conn = getConnection();
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
	public String getLatestQueriedRate() throws Exception {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get the latest queried rate");
		try (final Connection conn = getConnection();
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
	public void pushExchangeRate(ExchangeRate exchangeRate) throws Exception {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "push latest exchange rate to exchange rate table : {}",
				exchangeRate.toJson());
		try (final Connection conn = getConnection();
			 final PreparedStatement statement = conn.prepareStatement(
					 "INSERT INTO exchange_rate (expirationTime,exchangeRateFile) VALUES(?,?::JSON)")) {
			statement.setLong(1, exchangeRate.getNextExpirationTimeInSeconds());
			statement.setObject(2, exchangeRate.toJson());
			statement.executeUpdate();
		}
	}

	@Override
	public void pushMidnightRate(ExchangeRate exchangeRate) throws Exception {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "push the midnight exchange rate to midnight rate table : {}",
				exchangeRate.toJson());
		try (final Connection conn = getConnection();
			 final PreparedStatement statement = conn.prepareStatement(
					 "INSERT INTO midnight_rate (expirationTime,exchangeRateFile) VALUES(?,?::JSON)")) {
			statement.setLong(1, exchangeRate.getNextExpirationTimeInSeconds());
			statement.setObject(2, exchangeRate.toJson());
			statement.executeUpdate();
		}
	}

	@Override
	public void pushQueriedRate(long expirationTime, String queriedRate) throws Exception {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "push the queried exchanges to queried rate table : {}:{}",
				expirationTime, queriedRate);
		try (final Connection conn = getConnection();
			 final PreparedStatement statement = conn.prepareStatement(
					 "INSERT INTO queried_rate (expirationTime,queriedrates) VALUES(?,?::JSON)")) {
			statement.setLong(1, expirationTime);
			statement.setObject(2, queriedRate);
			statement.executeUpdate();
		}
	}

	private Connection getConnection() throws SQLException {
		final String endpoint = this.params.getEndpoint();
		LOGGER.info(Exchange.EXCHANGE_FILTER, "Connecting to endpoint: {}", endpoint);
		return DriverManager.getConnection(endpoint,
				this.params.getUsername(),
				this.params.getPassword());
	}

}
