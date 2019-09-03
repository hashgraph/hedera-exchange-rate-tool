package com.hedera.services.exchange;

import com.hedera.services.exchange.database.AWSDBParams;
import com.hedera.services.exchange.database.ExchangeDB;
import com.hedera.services.exchange.database.ExchangeRateAWSRD;

public class ExchangeRateApi {

	public static String getLatest() throws Exception {
		final ExchangeDB exchangeDb = new ExchangeRateAWSRD(new AWSDBParams());
		return exchangeDb.getLatestExchangeRate().toJson();
	}
}
