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

	private final AWSDBParams params;

	public ExchangeRateAWSRD(final AWSDBParams params) {
		this.params = params;
	}

	@Override
	public ExchangeRate getLatestExchangeRate() throws Exception {
		try (final Connection conn = getConnection();
			 final Statement statement = conn.createStatement();
			 final ResultSet result = statement.executeQuery(LATEST_EXCHANGE_QUERY)) {
				if (result.next()) {
					return ExchangeRate.fromJson(result.getString(2));
				}

				return null;
		}
	}

	@Override
	public ExchangeRate getLatestMidnightExchangeRate() {
		return null;
	}

	@Override
	public void pushExchangeRate(ExchangeRate exchangeRate) throws Exception {
		try (final Connection conn = getConnection();
			 final PreparedStatement statement = conn.prepareStatement(
					 "INSERT INTO exchange_rate (expirationTime,exchangeRateFile) VALUES(?,?::JSON)")) {
			statement.setLong(1, exchangeRate.getNextExpirationTimeInSeconds());
			statement.setObject(2, exchangeRate.toJson());
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
