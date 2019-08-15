package com.hedera.services.exchange;

import com.hedera.services.exchange.exchanges.Bitrex;
import com.hedera.services.exchange.exchanges.Coinbase;
import com.hedera.services.exchange.exchanges.Exchange;
import com.hedera.services.exchange.exchanges.Liquid;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * This class implements the methods that we perform periodically to generate Exchange rate
 */
public class ERTproc implements Callable<Double> {

    private static final Logger log = LogManager.getLogger(ERTproc.class);

    private static final List<Supplier<Exchange>> EXCHANGE_SUPPLIERS = Arrays.asList(Bitrex::load, Liquid::load, Coinbase::load);

    private String privateKey;
    private List<String> exchangeAPIList;
    private String mainNetAPI;
    private String pricingDBAPI;
    private Double maxDelta;
    private Double prevMedian;
    private Double currMedian;
    private String hederaFileIdentifier;

    public ERTproc(final String privateKey,
            final List<String> exchangeAPIList,
            final String mainNetAPI,
            final String pricingDBAPI,
            final Double maxDelta,
            final Double prevMedian,
            final Double currMedian,
            final String hederaFileIdentifier) {
        this.privateKey = privateKey;
        this.exchangeAPIList = exchangeAPIList;
        this.mainNetAPI = mainNetAPI;
        this.pricingDBAPI = pricingDBAPI;
        this.maxDelta = maxDelta;
        this.prevMedian = prevMedian;
        this.currMedian = currMedian;
        this.hederaFileIdentifier = hederaFileIdentifier;
    }

    // now that we have all the data/APIs required, add methods to perform the functions
    @Override
    public Double call() {
        // we call the methods in the order of execution logic
        log.log(Level.INFO, "Start of ERT Logic");

        // Make a list of exchanges
        log.log(Level.INFO, "generating exchange objects");
        final List<Exchange> exchanges = generateExchanges();

        // walk through each exchange object and calculate the median exchange rate
        return calculateMedianRate(exchanges);
    }

    private Double calculateMedianRate(final List<Exchange> exchanges) {
        log.log(Level.INFO, "sort the exchange list according to the exchange rate");

        exchanges.removeIf(x -> x.getHBarValue() == null || x.getHBarValue() == 0);

        // sort the exchange list on the basis of their exchange rate
        exchanges.sort(Comparator.comparingDouble(Exchange::getHBarValue));

        log.log(Level.INFO, "find the median");
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


}
