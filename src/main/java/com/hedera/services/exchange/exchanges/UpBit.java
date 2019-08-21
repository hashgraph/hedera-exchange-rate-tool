package com.hedera.services.exchange.exchanges;

import com.hedera.services.exchange.ERTproc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

public class UpBit extends AbstractExchange {
    private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);


    @Override
    public Double getHBarValue() {
        return null;
    }

    public static UpBit load(final String endpoint) {
        LOGGER.debug("Loading exchange rate from UpBit");
        return load(endpoint, UpBit.class);
    }
}
