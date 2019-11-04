package com.hedera.services.exchange;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hederahashgraph.api.proto.java.ExchangeRateSet;
import com.hederahashgraph.api.proto.java.TimestampSeconds;

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
	 * Converts the ExchangeRate object into a Json String using OBJECT_MAPPER
	 * @return Json String
	 * @throws JsonProcessingException
	 */
	public String toJson() throws JsonProcessingException {
		final ExchangeRate[] rates = new ExchangeRate[] { this };
		return OBJECT_MAPPER.writeValueAsString(rates);
	}

	/**
	 * Converts a Json string into an ExchangeRate object using OBJECT_MAPPER
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
		final com.hederahashgraph.api.proto.java.ExchangeRate currentRate =
				com.hederahashgraph.api.proto.java.ExchangeRate.newBuilder()
						.setExpirationTime(TimestampSeconds.newBuilder()
								.setSeconds(this.currentRate.getExpirationTimeInSeconds()
						).build())
						.setCentEquiv((int)this.currentRate.getCentEquiv())
						.setHbarEquiv((int)this.currentRate.getHBarEquiv())
						.build();

		final com.hederahashgraph.api.proto.java.ExchangeRate nextRate =
				com.hederahashgraph.api.proto.java.ExchangeRate.newBuilder()
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
		expiration.setTimeInMillis(this.getNextExpirationTimeInSeconds() * 1000);
		return expiration.get(Calendar.HOUR_OF_DAY) == 0 && expiration.get(Calendar.MINUTE) == 0 && expiration.get(Calendar.SECOND) == 0;
	}
}
