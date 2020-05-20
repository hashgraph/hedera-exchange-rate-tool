package com.hedera.exchange.exchanges;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a UpBit Exchange response.
 *
 * @author Anirudh, Cesar
 */
public class UpBit extends AbstractExchange {

    @Override
    @JsonProperty("HBAR")
    public Double getHBarValue() {
        return null;
    }

    public static UpBit load(final String endpoint) {
        return load(endpoint, UpBit.class);
    }
}
