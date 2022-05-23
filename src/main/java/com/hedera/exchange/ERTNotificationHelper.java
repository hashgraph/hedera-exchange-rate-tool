package com.hedera.exchange;

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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.hedera.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.hedera.exchange.ERTUtils.getDecryptedEnvironmentVariableFromAWS;

public final class ERTNotificationHelper {
	public static final Logger LOGGER = LogManager.getLogger(ERTNotificationHelper.class);

	private ERTNotificationHelper() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Send an email to the SNS topic
	 * @param subject
	 * 			Subject of the Email
	 * @param message
	 * 			Content of the Email
	 */
	public static void publishMessage(final String subject, final String message, final String region) {
		try {
			AmazonSNSClient SNS_CLIENT = (AmazonSNSClient) AmazonSNSClientBuilder.standard()
					.withRegion(getValidRegion(region))
					.build();
			final String SNS_ARN = getDecryptedEnvironmentVariableFromAWS("SNS_ARN");
			SNS_CLIENT.publish(SNS_ARN, message, subject);
			SNS_CLIENT.shutdown();
		} catch (AmazonSNSException ex) {
			LOGGER.error(Exchange.EXCHANGE_FILTER, "subject length : {} \n message length : {}",
					subject.length(), message.length());
			LOGGER.error(Exchange.EXCHANGE_FILTER, "Failed to submit  {} : {} \n {}", subject, message, ex);
		}
	}

	private static Regions getValidRegion(final String region) {
		try {
			return Regions.fromName(region);
		} catch (IllegalArgumentException ex) {
			LOGGER.warn(Exchange.EXCHANGE_FILTER, "Invalid region provided : {}, deafulting ot us-east-1",
					region);
			return Regions.US_EAST_1;
		}
	}
}
