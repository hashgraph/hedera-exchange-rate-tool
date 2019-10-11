package com.hedera.services.exchange;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ERTParamsTestCases {

    @Test
    public void readCofiguration() throws Exception {
        final ERTParams ertParams = ERTParams.readConfig("src/test/resources/configs/config.json");
        assertEquals(20, ertParams.getBound());
        assertEquals("0.0.57", ertParams.getPayAccount());
        assertEquals("0.0.112", ertParams.getFileId());
        assertEquals(15_000, ertParams.getValidationDelayInMilliseconds());
    }

    @Test
    public void readCofigurationFail() throws Exception {
        final ERTParams ertParams = ERTParams.readConfig("src/test/resources/configs/config1.json");
        assertEquals(20, ertParams.getBound());
        assertEquals("0.0.57", ertParams.getPayAccount());
        assertEquals(null, ertParams.getFileId());
        assertEquals(15_000, ertParams.getValidationDelayInMilliseconds());
    }

}
