package com.hedera.services.exchange;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ERTParamsTestCases {

    @Test
    public void readCofiguration() throws Exception {
        ERTParams ertParams = ERTParams.readConfig();
        assertEquals(5.0, ertParams.getMaxDelta());
        assertEquals("path", ertParams.getPrivateKeyPath());
        assertEquals("0.0.57", ertParams.getPayAccount());
    }

}
