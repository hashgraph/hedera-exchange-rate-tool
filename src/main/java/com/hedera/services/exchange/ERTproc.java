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
    private final double currentExchangeRate;
    private long tE;

    public ERTproc(final Map<String, String> exchangeApis,
            final double maxDelta,
            final double currentExchangeRate,
            final long tE) {
        this.exchangeApis = exchangeApis;
        this.maxDelta = maxDelta;
        this.currentExchangeRate = currentExchangeRate;
        this.tE = tE;
    }

    public ExchangeRate call() {
        LOGGER.log(Level.DEBUG, "Start of ERT Logic");

        try {
            LOGGER.log(Level.DEBUG, "generating exchange objects");
            final List<Exchange> exchanges = generateExchanges();

            Double medianExRate = calculateMedianRate(exchanges);
            LOGGER.log(Level.DEBUG, "Median calculated : {}", medianExRate);
            if (medianExRate == null){
                LOGGER.log(Level.WARN, "None of the exchanges returned a valid exchange rate");
                return null;
            }

            tE = getCurrentExpirationTime() / 1000;
            final Rate currentRate = new Rate(currentExchangeRate, tE);
            Rate nextRate = new Rate(medianExRate, tE + 3600);

            LOGGER.log(Level.DEBUG, "validating the median");
            final boolean isValid = currentRate.isValid(maxDelta, nextRate);
            LOGGER.log(Level.DEBUG, "Median is Valid : {}", isValid);

            if (!isValid){
                // limit the value
                LOGGER.log(Level.ERROR, "Median is not Valid");
                LOGGER.log(Level.DEBUG, "Expected exchange rate should lie between {} and {}",
                        this::getMaxExchangeRate, this::getMaxExchangeRate );
                LOGGER.log(Level.DEBUG, "Exchange rate calculated : {}", medianExRate);

                if (medianExRate < currentExchangeRate){
                    LOGGER.log(Level.DEBUG, "setting the new exchange rate as acceptable lower limit");
                    medianExRate = getMinExchangeRate();
                }
                else{
                    LOGGER.log(Level.DEBUG, "setting the new exchange rate as acceptable higher limit");
                    medianExRate = getMaxExchangeRate();
                }
                nextRate = new Rate(medianExRate, tE + 3600);
            }
            final ExchangeRate exchangeRate = new ExchangeRate(currentRate, nextRate);

            // build the ER File
            // sign the file accordingly
            if (isValid){
                //follow the automatic process
            }
            else{
                //follow the manual process
            }
            // create a transaction for the network
            // POST it to the network and Pricing DB
            return  exchangeRate;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private long getCurrentExpirationTime() {
        final long currentTime = System.currentTimeMillis();
        return ( currentTime - (currentTime % 3600000) ) + 3600000;
    }

    private double getMaxExchangeRate() {
        return currentExchangeRate * ( 1 + (maxDelta / 100.0));
    }

    private double getMinExchangeRate() {
        return currentExchangeRate * ( 1 - (maxDelta / 100.0));
    }

    private Double calculateMedianRate(final List<Exchange> exchanges) {
        LOGGER.log(Level.DEBUG, "Computing median");

        exchanges.removeIf(x -> x == null || x.getHBarValue() == null || x.getHBarValue() == 0.0);

        if (exchanges.size() == 0){
            LOGGER.log(Level.WARN, "No valid exchange rates retrieved.");
            return null;
        }
        exchanges.sort(Comparator.comparingDouble(Exchange::getHBarValue));

        LOGGER.log(Level.DEBUG, "finding the median");
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
                LOGGER.warn("API {} not found", api.getKey());
                continue;
            }

            final String endpoint = api.getValue();
            final Exchange exchange = exchangeLoader.apply(endpoint);
            exchanges.add(exchange);

        }

        return exchanges;
    }
}
