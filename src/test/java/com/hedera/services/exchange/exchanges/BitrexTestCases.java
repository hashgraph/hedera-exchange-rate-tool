package com.hedera.services.exchange.exchanges;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BitrexTestCases {

	@Test
	public void retrieveBitrexDataTest() throws IOException {
		final Bitrex bitrex = Bitrex.load();
		bitrex.isSuccess();
		final List<Exchange> exchanges = new ArrayList<>();
		final List<Exchange> sortedExchanges = exchanges.stream()
				.filter(x -> Objects.nonNull(bitrex.getHBarValue()))
				.sorted(Comparator.comparingDouble(Exchange::getHBarValue))
				.collect(Collectors.toList());

	}
}
