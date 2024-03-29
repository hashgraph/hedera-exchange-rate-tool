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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedera.hashgraph.sdk.proto.ExchangeRateSet;
import com.hedera.hashgraph.sdk.proto.TimestampSeconds;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * This Class represents the Exchange Rate File that we send to the nodes.
 *
 * @author Anirudh, Cesar
 */
public class ExchangeRate {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@JsonProperty("CurrentRate")
	private Rate currentRate;

	@JsonProperty("NextRate")
	private Rate nextRate;


	@JsonCreator
	public ExchangeRate(@JsonProperty("CurrentRate") final Rate currentRate, @JsonProperty("NextRate") final Rate nextRate) {
		this.currentRate = currentRate;
		this.nextRate = nextRate;
	}

	@JsonIgnore
	public long getCurrentExpirationsTimeInSeconds() {
		return this.currentRate.getExpirationTimeInSeconds();
	}

	@JsonIgnore
	public long getNextExpirationTimeInSeconds() {
		return this.nextRate.getExpirationTimeInSeconds();
	}

	@JsonIgnore
	public Rate getCurrentRate() {
		return this.currentRate;
	}

	@JsonIgnore
	public Rate getNextRate() {
		return this.nextRate;
	}

	/**
	 * Converts the ExchangeRate object into a Json String
	 * @return Json String
	 * @throws JsonProcessingException
	 */
	public String toJson() throws JsonProcessingException {
		final ExchangeRate[] rates = new ExchangeRate[] { this };
		return OBJECT_MAPPER.writeValueAsString(rates);
	}

	/**
	 * Converts a Json string into an ExchangeRate object
	 * @param json String that represents a exchange rate file.
	 * @return ExchangeRate object
	 * @throws IOException
	 */
	public static ExchangeRate fromJson(final String json) throws IOException {
		try {
			final ExchangeRate[] rates = OBJECT_MAPPER.readValue(json, ExchangeRate[].class);
			return rates[0];
		} catch (final Exception ex) {
			return OBJECT_MAPPER.readValue(json, ExchangeRate.class);
		}
	}

	/**
	 * Converts the ExchangeRate object into ExchangeRateSet that is used to send in the transaction that we submit to
	 * the nodes.
	 *
	 * @return ExchangeRateSet
	 */
	public ExchangeRateSet toExchangeRateSet() {
		final com.hedera.hashgraph.sdk.proto.ExchangeRate currentRate =
				com.hedera.hashgraph.sdk.proto.ExchangeRate.newBuilder()
						.setExpirationTime(TimestampSeconds.newBuilder()
								.setSeconds(this.currentRate.getExpirationTimeInSeconds()
						).build())
						.setCentEquiv((int)this.currentRate.getCentEquiv())
						.setHbarEquiv((int)this.currentRate.getHBarEquiv())
						.build();

		final com.hedera.hashgraph.sdk.proto.ExchangeRate nextRate =
				com.hedera.hashgraph.sdk.proto.ExchangeRate.newBuilder()
						.setExpirationTime(TimestampSeconds.newBuilder()
								.setSeconds(this.nextRate.getExpirationTimeInSeconds()
								).build())
						.setCentEquiv((int)this.nextRate.getCentEquiv())
						.setHbarEquiv((int)this.nextRate.getHBarEquiv())
						.build();

		return ExchangeRateSet.newBuilder().setCurrentRate(currentRate).setNextRate(nextRate).build();
	}

	/**
	 * Checks if the current hour ends on the midnight.
	 */
	@JsonIgnore
	public boolean isMidnightTime(){
		Calendar expiration = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
		expiration.setTimeInMillis(this.getCurrentExpirationsTimeInSeconds() * 1000);
		return expiration.get(Calendar.HOUR_OF_DAY) == 0 && expiration.get(Calendar.MINUTE) == 0 && expiration.get(Calendar.SECOND) == 0;
	}
}
