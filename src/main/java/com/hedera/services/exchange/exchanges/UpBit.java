package com.hedera.services.exchange.exchanges;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UpBit extends AbstractExchange {
    private static final Logger LOGGER = LogManager.getLogger(UpBit.class);


    @Override
    public Double getHBarValue() {
        return null;
    }

    public static UpBit load(final String endpoint) {
        LOGGER.debug("Loading exchange rate from UpBit");
        return load(endpoint, UpBit.class);
    }
}
