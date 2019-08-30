package com.hedera.services.exchange;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hedera.hashgraph.sdk.proto.ExchangeRateSet;
import com.hedera.hashgraph.sdk.proto.TimestampSeconds;

import java.util.Date;

@JsonRootName("exchangeRate")
public class ExchangeRate {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.WRAP_ROOT_VALUE);;

	@JsonProperty("currentRate")
	private Rate currentRate;

	@JsonProperty("nextRate")
	private Rate nextRate;

	public ExchangeRate(final Rate currentRate, final Rate nextRate) {
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

	public String toJson() throws JsonProcessingException {
		return OBJECT_MAPPER.writeValueAsString(this);
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

	public boolean isMidnightTime(){
		Date expiration = new Date( this.getNextExpirationTimeInSeconds() * 1000);

		if(expiration.getHours() == 0 && expiration.getMinutes() == 0 && expiration.getSeconds() == 0){
			return true;
		}
		else{
			return false;
		}
	}
}
