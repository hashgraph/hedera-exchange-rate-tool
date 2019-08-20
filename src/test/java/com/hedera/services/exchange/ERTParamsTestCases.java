package com.hedera.services.exchange;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ERTParamsTestCases {

    @Test
    public void readCofiguration() throws Exception {
        final ERTParams ertParams = ERTParams.readConfig("src/test/resources/configs/config.json");
        assertEquals(5.0, ertParams.getMaxDelta());
        assertEquals("path", ertParams.getPrivateKeyPath());
        assertEquals("0.0.57", ertParams.getPayAccount());
        assertEquals("0.0.112", ertParams.getFileIdentifier());
        assertEquals(3600, ertParams.getFrequencyInSeconds());
        assertEquals("", ertParams.toJson());
    }

    @Test
    public void readCofigurationFail() throws Exception {
        final ERTParams ertParams = ERTParams.readConfig("src/test/resources/configs/config1.json");
        assertEquals(5.0, ertParams.getMaxDelta());
        assertEquals("path", ertParams.getPrivateKeyPath());
        assertEquals("0.0.57", ertParams.getPayAccount());
        assertEquals(null, ertParams.getFileIdentifier());
        assertEquals(0, ertParams.getFrequencyInSeconds());
        assertEquals("", ertParams.toJson());
    }

}
