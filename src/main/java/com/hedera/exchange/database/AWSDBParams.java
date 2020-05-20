package com.hedera.exchange.database;

import com.hedera.exchange.ExchangeRateUtils;

public class AWSDBParams {
	public String getEndpoint() {
		return ExchangeRateUtils.getDecryptedEnvironmentVariableFromAWS("ENDPOINT") + getDatabaseName();
	}

	public String getUsername() {
		return ExchangeRateUtils.getDecryptedEnvironmentVariableFromAWS("USERNAME");
	}

	public String getPassword() {
		return ExchangeRateUtils.getDecryptedEnvironmentVariableFromAWS("PASSWORD");
	}

	public String getDatabaseName() {
		return ExchangeRateUtils.getDecryptedEnvironmentVariableFromAWS("DATABASE");
	}
}
