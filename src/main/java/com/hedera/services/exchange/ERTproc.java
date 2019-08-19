package com.hedera.services.exchange;

import com.hedera.services.exchange.exchanges.Bitrex;
import com.hedera.services.exchange.exchanges.Coinbase;
import com.hedera.services.exchange.exchanges.Exchange;
import com.hedera.services.exchange.exchanges.Liquid;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * This class implements the methods that we perform periodically to generate Exchange rate
 */
public class ERTproc {

    private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);

    private static final List<Supplier<Exchange>> EXCHANGE_SUPPLIERS = Arrays.asList(Bitrex::load, Liquid::load, Coinbase::load);

    private String privateKey;
    private List<String> exchangeAPIList;
    private String mainNetAPI;
    private String pricingDBAPI;
    private double maxDelta;
    private double erNow;
    private double erNew;
    private long tE;
    private String hederaFileIdentifier;

    public ERTproc(final String privateKey,
            final List<String> exchangeAPIList,
            final String mainNetAPI,
            final String pricingDBAPI,
            final double maxDelta,
            final double erNow,
            final long tE,
            final String hederaFileIdentifier) {
        this.privateKey = privateKey;
        this.exchangeAPIList = exchangeAPIList;
        this.mainNetAPI = mainNetAPI;
        this.pricingDBAPI = pricingDBAPI;
        this.maxDelta = maxDelta;
        this.erNow = erNow;
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
            if ( medianExRate == null ){
                return null;
            }

            tE = getCurrentExpirationTime() / 1000;
            final Rate currentRate = new Rate(erNow, tE);
            Rate nextRate = new Rate(medianExRate, tE + 3600);

            LOGGER.log(Level.INFO, "validate the median");
            final boolean isValid = currentRate.isValid(maxDelta, nextRate);

            if (!isValid){
                // limit the value
                if (medianExRate < erNow){
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
        long currentTime = System.currentTimeMillis();
        long nextHour = ( currentTime - (currentTime % 3600000) ) + 3600000;
        return nextHour;
    }

    private double getMaxER() {
        return erNow * ( 1 + ( (double)maxDelta / 100 ));
    }

    private double getMinER() {
        return erNow * ( 1 - ( (double)maxDelta / 100 ));
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
        // since we have fixed exchanges, we create an object for each exchange type ,
        // retrieve the exchange rate and add it to the list.
        final List<Exchange> exchanges = new ArrayList<>();
        for (final Supplier<Exchange> exchangeSupplier : EXCHANGE_SUPPLIERS) {
            exchanges.add(exchangeSupplier.get());
        }

        return exchanges;
    }

    public static void main (String ... args) {
        try {
            final ERTproc proc = new ERTproc("0",
                    null,
                    "0",
                    "0",
                    0.0,
                    0.0,
                    0l,
                    "0");
            proc.call();
        } catch (final Exception ex) {
            LOGGER.error("Error whiile running ERTPROC {}", ex);
        }
    }

    public static ExchangeRate execute(final String ... input) {
        try {
            final ERTproc proc = new ERTproc("0",
                    null,
                    "0",
                    "0",
                    0.0,
                    0.0,
                    0l,
                    "0");
            return proc.call();
        } catch (final Exception ex) {
            LOGGER.error("Error whiile running ERTPROC {}", ex);
            return null;
        }
    }
}
