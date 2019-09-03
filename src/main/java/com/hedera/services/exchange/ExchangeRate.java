package com.hedera.services.exchange;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedera.hashgraph.sdk.proto.ExchangeRateSet;
import com.hedera.hashgraph.sdk.proto.TimestampSeconds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

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
	public long getCurrentExpiriationsTimeInSeconds() {
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

	public String toJson() throws JsonProcessingException {
		final ExchangeRate[] rates = new ExchangeRate[] { this };
		return OBJECT_MAPPER.writeValueAsString(rates);
	}

	public static ExchangeRate fromJson(final String json) throws IOException {
		return OBJECT_MAPPER.readValue(json, ExchangeRate.class);
	}

	public ExchangeRateSet toExchangeRateSet() {
		final com.hedera.hashgraph.sdk.proto.ExchangeRate currentRate =
				com.hedera.hashgraph.sdk.proto.ExchangeRate.newBuilder()
						.setExpirationTime(TimestampSeconds.newBuilder()
								.setSeconds(this.currentRate.getExpirationTimeInSeconds()
						).build())
						.setCentEquiv(this.currentRate.getCentEquiv())
						.setHbarEquiv(this.currentRate.getHBarEquiv())
						.build();

		final com.hedera.hashgraph.sdk.proto.ExchangeRate nextRate =
				com.hedera.hashgraph.sdk.proto.ExchangeRate.newBuilder()
						.setExpirationTime(TimestampSeconds.newBuilder()
								.setSeconds(this.nextRate.getExpirationTimeInSeconds()
								).build())
						.setCentEquiv(this.nextRate.getCentEquiv())
						.setHbarEquiv(this.nextRate.getHBarEquiv())
						.build();

		return ExchangeRateSet.newBuilder().setCurrentRate(currentRate).setNextRate(nextRate).build();
	}

	@JsonIgnore
	public boolean isMidnightTime(){
		Calendar expiration = GregorianCalendar.getInstance();
		expiration.setTimeInMillis(this.getNextExpirationTimeInSeconds() * 1000);
		return expiration.get(Calendar.HOUR_OF_DAY) == 0 && expiration.get(Calendar.MINUTE) == 0 && expiration.get(Calendar.SECOND) == 0;
	}

	@JsonIgnore
	public double getNextRatecentEqu(){
		return nextRate.getHBarValueInDecimal();
	}
}
