package com.hedera.exchange;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExchangeRateUtilsTestCases {

	@Test
	@Ignore
	public void getDecryptedEnvironmentVariableFromAWSTest() {
		final String expectedValue = "https://s3.amazonaws.com/exchange.rate.config.integration/config.json";
		final String encryptedValue = "AQICAHi3BYYdRzjj1ZR5ij/3mN6+GWqEbw7NTAG0fm7nzYo3MwHyBlKsmA+1lepLUe" +
				"+0rgeFAAAApzCBpAYJKoZIhvcNAQcGoIGWMIGTAgEAMIGNBgkqhkiG9w0BBwEwHgYJYIZIAWUDBAEuMBEEDLHJjkIANloMVIhCdgIBEIBg9HnXBKnxE3c4H5/17ilQR0G6DqZKH6dzBnhkUAjYbg1sBuStjVA8rQwBUtiSKO7b5ehQh+OxnrJxVbHAZNylSH71fr7OICMI3iA2qkIM8gtWNG1htphGhkDLCRcaw5Xh";
		final String lambdaFunctioName = "exchange-rate-tool-lambda-integration";
		assertEquals(expectedValue, ExchangeRateUtils.getDecryptedValueFromAWS(encryptedValue, lambdaFunctioName));
	}
}
