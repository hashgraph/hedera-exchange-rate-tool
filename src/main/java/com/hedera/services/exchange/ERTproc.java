package com.hedera.services.exchange;

import com.hedera.services.exchange.exchanges.Bitrex;
import com.hedera.services.exchange.exchanges.Coinbase;
import com.hedera.services.exchange.exchanges.Exchange;
import com.hedera.services.exchange.exchanges.Liquid;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * This class implements the methods that we perform periodically to generate Exchange rate
 */
public class ERTproc implements Runnable {
    // logger object to write logs into
    private static final Logger log = LogManager.getLogger(ERTproc.class);

    private String privateKey;
    private List<String> exchangeAPIList;
    private List<Exchange> exchangeList;
    private String mainNetAPI;
    private String pricingDBAPI;
    private Double maxDelta;
    private Double erNow;
    private Double erNew;
    private Double tE;
    private String hederaFileIdentifier;

    public ERTproc(final String privateKey,
            final List<String> exchangeAPIList,
            final String mainNetAPI,
            final String pricingDBAPI,
            final Double maxDelta,
            final Double erNow,
            final Double tE,
            final String hederaFileIdentifier) {
        this.privateKey = privateKey;
        this.exchangeAPIList = exchangeAPIList;
        this.exchangeList = new ArrayList<>();
        this.mainNetAPI = mainNetAPI;
        this.pricingDBAPI = pricingDBAPI;
        this.maxDelta = maxDelta;
        this.erNow = erNow;
        this.tE = tE;
        this.hederaFileIdentifier = hederaFileIdentifier;
    }

    // now that we have all the data/APIs required, add methods to perform the functions
    @Override
    public void run() {
        // we call the methods in the order of execution logic
        log.log(Level.INFO, "Start of ERT Logic");

        // Make a list of exchanges
        try {
            log.log(Level.INFO, "generating exchange objects");
            this.exchangeList = generateExchanges();

            log.log(Level.INFO, "Calculating median");
            Double medianExRate = calculateMedianRate();
            log.log(Level.DEBUG, "Median calculated : " + medianExRate);

            Rate currentRate = new Rate("CurrentRate", 1, erNow, tE);
            Rate nextRate = new Rate("NextRate", 1, medianExRate, tE+3600);

            ERF exchangeRateFileObject = new ERF(currentRate, nextRate);

            // Check delta
            // sign the file accordingly
            // POST it to the network and Pricing DB

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Double calculateMedianRate() {
        Double median = 0.0;
        log.log(Level.INFO, "sort the exchange list according to the exchange rate");

        exchangeList.removeIf(x -> x.getHBarValue() == null || x.getHBarValue() == 0);

        // sort the exchange list on the basis of their exchange rate
        Collections.sort(exchangeList, Comparator.comparingDouble(Exchange::getHBarValue));

        log.log(Level.INFO, "find the median");
        if ( exchangeList.size() %2 == 0 ) {
            median = (exchangeList.get(exchangeList.size() / 2).getHBarValue() + exchangeList.get(exchangeList.size() / 2 - 1).getHBarValue()) / 2;
        }
        else {
            median = exchangeList.get(exchangeList.size() / 2).getHBarValue();
        }

        return median;
    }

    private List<Exchange> generateExchanges() throws IOException {
        // since we have fixed exchanges, we create an object for each exchange type ,
        // retrieve the exchange rate and add it to the list.
        List<Exchange> exchangeList = new ArrayList<Exchange>();
        log.log(Level.INFO, "Adding Bitrex");
        Bitrex birex = Bitrex.load();
        exchangeList.add(birex);
        log.log(Level.INFO, "Adding Liquid");
        Liquid liquid = Liquid.load();
        exchangeList.add(liquid);
        log.log(Level.INFO, "Adding Coinbase");
        Coinbase coinbase = Coinbase.load();
        exchangeList.add(coinbase);

        return exchangeList;
    }


}
