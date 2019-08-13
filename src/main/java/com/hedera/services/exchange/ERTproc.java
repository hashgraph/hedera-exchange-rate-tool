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

    private String m_privateKey;
    private List<String> m_exchangeAPIList;
    private String m_mainNetAPI;
    private String m_pricingDBAPI;
    private Double m_maxDelta;
    private Double m_prevMedian;
    private Double m_currMedian;
    private String m_hederaFileIdentifier;

    public ERTproc(final String m_privateKey,
            final List<String> m_exchangeAPIList,
            final String m_mainNetAPI,
            final String m_pricingDBAPI,
            final Double m_maxDelta,
            final Double m_prevMedian,
            final Double m_currMedian,
            final String m_hederaFileIdentifier) {
        this.m_privateKey = m_privateKey;
        this.m_exchangeAPIList = m_exchangeAPIList;
        this.m_mainNetAPI = m_mainNetAPI;
        this.m_pricingDBAPI = m_pricingDBAPI;
        this.m_maxDelta = m_maxDelta;
        this.m_prevMedian = m_prevMedian;
        this.m_currMedian = m_currMedian;
        this.m_hederaFileIdentifier = m_hederaFileIdentifier;
    }

    // now that we have all the data/APIs required, add methods to perform the functions
    @Override
    public void run() {
        // we call the methods in the order of execution logic
    }
}
