package com.hedera.services.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hedera.services.exchange.exchanges.*;
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
        EXCHANGES.put("okcoin", OkCoin::load);
    }

    private final Map<String, String> exchangeApis;
    private final long bound;
    private final long floor;
    private List<Exchange> exchanges;
    private final Rate midnightExchangeRate;
    private final Rate currentExchangeRate;
    private final long hbarEquiv;
    private final long frequencyInSeconds;

    public ERTproc(final long hbarEquiv,
            final Map<String, String> exchangeApis,
            final long bound,
            final long floor,
            final Rate midnightExchangeRate,
            final Rate currentExchangeRate,
            final long frequencyInSeconds) {
        this.hbarEquiv = hbarEquiv;
        this.exchangeApis = exchangeApis;
        this.bound = bound;
        this.floor = floor;
        this.midnightExchangeRate = midnightExchangeRate;
        this.currentExchangeRate = currentExchangeRate;
        this.frequencyInSeconds = frequencyInSeconds;
    }

    public ExchangeRate call() {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Start of ERT Logic");

        try {
            LOGGER.info(Exchange.EXCHANGE_FILTER, "Generating exchange objects");
            exchanges = generateExchanges();
            currentExchangeRate.setExpirationTime(ERTParams.getCurrentExpirationTime());
            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Setting next hour as current expiration time :{}",
                    currentExchangeRate.getExpirationTimeInSeconds());
            final long nextExpirationTimeInSeconds = currentExchangeRate.getExpirationTimeInSeconds() + frequencyInSeconds;

            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Setting next-next hour as next expiration time :{}",
                    nextExpirationTimeInSeconds);

            final Double medianExRate = calculateMedianRate(exchanges);
            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Median calculated : {}", medianExRate);
            if (medianExRate == null){
                LOGGER.warn(Exchange.EXCHANGE_FILTER, "No median computed. Using current rate as next rate: {}",
                        this.currentExchangeRate.toJson());
                final Rate nextRate = new Rate(this.currentExchangeRate.getHBarEquiv(),
                        this.currentExchangeRate.getCentEquiv() ,
                        nextExpirationTimeInSeconds);
                return new ExchangeRate(this.currentExchangeRate, nextRate);
            }

            Rate nextRate = new Rate(this.hbarEquiv,
                    (int) (medianExRate * 100 * this.hbarEquiv),
                    nextExpirationTimeInSeconds);

            if(midnightExchangeRate != null && !midnightExchangeRate.isSmallChange(bound, nextRate)) {
                LOGGER.debug(Exchange.EXCHANGE_FILTER, "last midnight value present. Validating the median with {}",
                        midnightExchangeRate.toJson());
                    nextRate = midnightExchangeRate.clipRate(nextRate, this.bound, floor);
            } else {
                LOGGER.debug(Exchange.EXCHANGE_FILTER, "No midnight value found. " +
                        "Skipping validation of the calculated median");
            }

            return new ExchangeRate(currentExchangeRate, nextRate);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Double calculateMedianRate(final List<Exchange> exchanges) {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Computing median");

        exchanges.removeIf(x -> x == null || x.getHBarValue() == null || x.getHBarValue() == 0.0);

        if (exchanges.size() == 0){
            LOGGER.error(Exchange.EXCHANGE_FILTER, "No valid exchange rates retrieved.");
            return null;
        }

        exchanges.sort(Comparator.comparingDouble(Exchange::getHBarValue));
        LOGGER.info(Exchange.EXCHANGE_FILTER, "find the median");
        if (exchanges.size() % 2 == 0 ) {
            return (exchanges.get(exchanges.size() / 2).getHBarValue() +
                    exchanges.get(exchanges.size() / 2 - 1).getHBarValue()) / 2;
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
                LOGGER.error(Exchange.EXCHANGE_FILTER,"API {} not found", api.getKey());
                continue;
            }

            final String endpoint = api.getValue();
            final Exchange exchange = exchangeLoader.apply(endpoint);
            if (exchange == null) {
                LOGGER.error(Exchange.EXCHANGE_FILTER,"API {} not loaded", api.getKey());
                continue;
            }

            exchanges.add(exchange);
        }

        return exchanges;
    }

    public String getExchangeJson() throws JsonProcessingException {
        return Exchange.OBJECT_MAPPER.writeValueAsString(exchanges);
    }
}
