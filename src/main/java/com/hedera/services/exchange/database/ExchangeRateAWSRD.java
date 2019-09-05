package com.hedera.services.exchange.database;

import com.hedera.services.exchange.ExchangeRate;
import com.hedera.services.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ExchangeRateAWSRD implements ExchangeDB {

	private static final Logger LOGGER = LogManager.getLogger(ExchangeRateAWSRD.class);

	private static final String LATEST_EXCHANGE_QUERY = "SELECT e1.expirationTime, e1.exchangeRateFile FROM exchange_rate AS e1 INNER JOIN (SELECT MAX(expirationTime) expirationTime FROM exchange_rate) AS e2 ON e1.expirationTime = e2.expirationTime LIMIT 1";

	private static final String MIDNIGHT_EXCHANGE_QUERY = "SELECT e1.expirationTime, e1.exchangeRateFile FROM midnight_rate AS e1 INNER JOIN (SELECT MAX(expirationTime) expirationTime FROM midnight_rate) AS e2 ON e1.expirationTime = e2.expirationTime LIMIT 1";

	private final AWSDBParams params;

	public ExchangeRateAWSRD(final AWSDBParams params) {
		this.params = params;
	}

	@Override
	public ExchangeRate getLatestExchangeRate() throws Exception {
		LOGGER.info(Exchange.EXCHANGE_FILTER, "query to get latest exchange rate from exchange rate table");
		try (final Connection conn = getConnection();
			 final Statement statement = conn.createStatement();
			 final ResultSet result = statement.executeQuery(LATEST_EXCHANGE_QUERY)) {
				if (result.next()) {
					return ExchangeRate.fromJson(result.getString(2));
				}
			LOGGER.warn(Exchange.EXCHANGE_FILTER, "failed to get latest exchange rate from exchange rate table ");
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
				return ExchangeRate.fromJson(result.getString(2));
			}
			LOGGER.warn(Exchange.EXCHANGE_FILTER, "failed to get latest exchange rate from midnight rate table ");
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
