package com.hedera.services.exchange.database;

import com.hedera.services.exchange.ExchangeRateUtils;

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
