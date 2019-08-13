package com.hedera.services.exchange.exchanges;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BitrexTestCases {

	@Test
	public void retrieveBitrexDataTest() throws IOException {
		final Bitrex bitrex = Bitrex.load();
		bitrex.isSuccess();
		final List<Exchange> exchanges = new ArrayList<>();
		exchanges.sort(Comparator.comparingDouble(Exchange::getHBarValue));
	}
}
