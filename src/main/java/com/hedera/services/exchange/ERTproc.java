package com.hedera.services.exchange;

import com.hedera.services.exchange.exchanges.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * This class implements the methods that we perform periodically to generate Exchange rate
 */
public class ERTproc {

    private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);

    private static final Map<String, Function<String, Exchange>> EXCHANGES = new HashMap<>();

    static {
        EXCHANGES.put("bitrex", Bitrex::load);
        EXCHANGES.put("liquid", Liquid::load);
        EXCHANGES.put("coinbase", Coinbase::load);
        EXCHANGES.put("upbit", UpBit::load);
    }

    private final Map<String, String> exchangeApis;
    private final double maxDelta;
    private final Rate currentExchangeRate;
    private final int hbarEquiv;

    public ERTproc(final int hbarEquiv,
            final Map<String, String> exchangeApis,
            final double maxDelta,
            final Rate currentExchangeRate) {
        this.hbarEquiv = hbarEquiv;
        this.exchangeApis = exchangeApis;
        this.maxDelta = maxDelta;
        this.currentExchangeRate = currentExchangeRate;
    }

    public ExchangeRate call() {
        LOGGER.log(Level.INFO, "Start of ERT Logic");

        try {
            LOGGER.log(Level.INFO, "Generating exchange objects");
            final List<Exchange> exchanges = generateExchanges();

            Double medianExRate = calculateMedianRate(exchanges);
            LOGGER.log(Level.DEBUG, "Median calculated : " + medianExRate);
            if (medianExRate == null){
                return null;
            }

            final long nextExpirationTimeInSeconds = currentExchangeRate.getExpirationTimeInSeconds() + 3_600;
            Rate nextRate = new Rate(this.hbarEquiv, medianExRate, nextExpirationTimeInSeconds);

            if (!currentExchangeRate.isValid(maxDelta, nextRate)){
                if (this.currentExchangeRate.compareTo(medianExRate) > 0) {
                    medianExRate = this.currentExchangeRate.getMinExchangeRate(maxDelta);
                } else {
                    medianExRate = this.currentExchangeRate.getMaxExchangeRate(maxDelta);
                }

                nextRate = new Rate(this.hbarEquiv, medianExRate, nextExpirationTimeInSeconds);
            }

            return new ExchangeRate(currentExchangeRate, nextRate);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Double calculateMedianRate(final List<Exchange> exchanges) {
        LOGGER.log(Level.INFO, "Computing median");

        exchanges.removeIf(x -> x == null || x.getHBarValue() == null || x.getHBarValue() == 0.0);

        if (exchanges.size() == 0){
            LOGGER.log(Level.ERROR, "No valid exchange rates retrieved.");
            return null;
        }
        exchanges.sort(Comparator.comparingDouble(Exchange::getHBarValue));

        LOGGER.log(Level.INFO, "find the median");
        if (exchanges.size() % 2 == 0 ) {
            return (exchanges.get(exchanges.size() / 2).getHBarValue() + exchanges.get(exchanges.size() / 2 - 1).getHBarValue()) / 2;
        }
        else {
            return exchanges.get(exchanges.size() / 2).getHBarValue();
        }
    }

    private List<Exchange> generateExchanges() {
        final List<Exchange> exchanges = new ArrayList<>();

        for (final Map.Entry<String, String> api : this.exchangeApis.entrySet()) {
            final Function<String, Exchange> exchangeLoader = EXCHANGES.get(api.getKey());
            if (exchangeLoader == null) {
                LOGGER.error("API {} not found", api.getKey());
                continue;
            }

            final String endpoint = api.getValue();
            final Exchange exchange = exchangeLoader.apply(endpoint);
            if (exchange == null) {
                LOGGER.error("API {} not loaded", api.getKey());
                continue;
            }

            exchanges.add(exchange);
        }

        return exchanges;
    }
}
