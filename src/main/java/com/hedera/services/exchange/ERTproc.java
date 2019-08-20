package com.hedera.services.exchange;

import com.hedera.services.exchange.exchanges.Bitrex;
import com.hedera.services.exchange.exchanges.Coinbase;
import com.hedera.services.exchange.exchanges.Exchange;
import com.hedera.services.exchange.exchanges.Liquid;
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
    }

    private final String privateKey;
    private final Map<String, String> exchangeApis;
    private final String mainNetAPI;
    private final double maxDelta;
    private final double currentExchangeRate;
    private long tE;
    private String hederaFileIdentifier;

    public ERTproc(final String privateKey,
            final Map<String, String> exchangeApis,
            final String mainNetAPI,
            final double maxDelta,
            final double currentExchangeRate,
            final long tE,
            final String hederaFileIdentifier) {
        this.privateKey = privateKey;
        this.exchangeApis = exchangeApis;
        this.mainNetAPI = mainNetAPI;
        this.maxDelta = maxDelta;
        this.currentExchangeRate = currentExchangeRate;
        this.tE = tE;
        this.hederaFileIdentifier = hederaFileIdentifier;
    }

    // now that we have all the data/APIs required, add methods to perform the functions
    public ExchangeRate call() {
        // we call the methods in the order of execution logic
        LOGGER.log(Level.INFO, "Start of ERT Logic");

        // Make a list of exchanges
        try {
            LOGGER.log(Level.INFO, "generating exchange objects");
            final List<Exchange> exchanges = generateExchanges();

            LOGGER.log(Level.INFO, "Calculating median");
            Double medianExRate = calculateMedianRate(exchanges);
            LOGGER.log(Level.DEBUG, "Median calculated : " + medianExRate);
            if (medianExRate == null){
                return null;
            }

            tE = getCurrentExpirationTime() / 1000;
            final Rate currentRate = new Rate(currentExchangeRate, tE);
            Rate nextRate = new Rate(medianExRate, tE + 3600);

            LOGGER.log(Level.INFO, "validate the median");
            final boolean isValid = currentRate.isValid(maxDelta, nextRate);

            if (!isValid){
                // limit the value
                if (medianExRate < currentExchangeRate){
                    medianExRate = getMinER();
                }
                else{
                    medianExRate = getMaxER();
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

    private double getMaxER() {
        return currentExchangeRate * ( 1 + (maxDelta / 100.0));
    }

    private double getMinER() {
        return currentExchangeRate * ( 1 - (maxDelta / 100.0));
    }

    private Double calculateMedianRate(final List<Exchange> exchanges) {
        LOGGER.log(Level.INFO, "sort the exchange list according to the exchange rate");

        exchanges.removeIf(x -> x.getHBarValue() == null || x.getHBarValue() == 0.0);

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

            exchanges.add(exchangeLoader.apply(api.getValue()));
        }

        return exchanges;
    }
}
