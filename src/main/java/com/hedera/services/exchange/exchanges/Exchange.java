package com.hedera.services.exchange.exchanges;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public interface Exchange {

	Marker EXCHANGE_FILTER = MarkerManager.getMarker("EXCHANGE");

	ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
			false);
	Double getHBarValue();
	String getEndPoint();
}
