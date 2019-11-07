package com.hedera.services.exchange;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * This class implements a helper function to get the decrypted environment variables set in AWS
 *
 * @author Anirudh, Cesar
 */
public class ExchangeRateUtils {

	/**
	 * Get the decrypted Environment variable set in AWS
	 * for example: the DB endpoint, username, password to access the Database, config file path etc..
	 * @param environmentVariable - Encrypted variable
	 * @return decrypted Environment Variable.
	 */
	public static String getDecryptedEnvironmentVariableFromAWS(final String environmentVariable) {
		final byte[] encryptedKey = Base64.decode(System.getenv(environmentVariable));

		final AWSKMS client = AWSKMSClientBuilder.defaultClient();

		final DecryptRequest request = new DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(encryptedKey));
		final ByteBuffer plainTextKey = client.decrypt(request).getPlaintext();
		return new String(plainTextKey.array(), StandardCharsets.UTF_8);
	}
}
