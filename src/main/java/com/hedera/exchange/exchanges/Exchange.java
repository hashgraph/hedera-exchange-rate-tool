package com.hedera.exchange.exchanges;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * Exchange Interface for every Exchange type.
 * This gives a blueprint of all the methods that are to be implemented in every exchange class.
 *
 * @author Anirudh, Cesar
 */
public interface Exchange {

	Marker EXCHANGE_FILTER = MarkerManager.getMarker("EXCHANGE");



	ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
			false);

	Double getHBarValue();

	void setEndPoint(String url);

	String toJson() throws JsonProcessingException;
}
