package com.hedera.services.exchange;

import com.hedera.services.exchange.database.AWSDBParams;
import com.hedera.services.exchange.database.ExchangeRateAWSRD;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ExchangeRateToolTestCases {

	@ParameterizedTest
	@CsvSource({"src/test/resources/configs/configtestnet0.json"})
	public void test(final String configPath) throws Exception {
		new MockUp<ExchangeRateUtils>() {
			String getDecryptedEnvironmentVariableFromAWS(final String env) {
				if (ERTParams.OPERATOR_KEY_ENV_VAL.equals(env)) {
					return "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137";
				}

				return null;
			}
		};

		new MockUp<ExchangeRateAWSRD>() {

			@Mock
			void $init(final AWSDBParams params) {
			}

			@Mock
			ExchangeRate getLatestExchangeRate() {
				return null;
			}

			@Mock
			ExchangeRate getLatestMidnightExchangeRate() {
				return null;
			}
		};

		final String[] args = new String[] { configPath };
		ExchangeRateTool.main(args);
	}
}
