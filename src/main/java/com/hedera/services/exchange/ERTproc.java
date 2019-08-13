package com.hedera.services.exchange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * This class implements the methods that we perform periodically to generate Exchange rate
 */
public class ERTproc implements Runnable {
    // logger object to write logs into
    private static final Logger log = LogManager.getLogger(ERTproc.class);

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
    public void run() {
        // we call the methods in the order of execution logic
    }
}
