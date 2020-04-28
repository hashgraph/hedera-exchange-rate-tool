package com.hedera.services.exchange;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

public class ExchangeRateToolTestCases {
    private Map<String, String> nodes =  new HashMap<>();
    private String addressBook = "";

    @Test
    public void verifyNodesFromAddressBook(){
        setup();

        Map<String, String> ERTnodes = ExchangeRateTool.getNodesFromAddressBook(addressBook);
        for( String node : ERTnodes.keySet()){
            Assert.assertEquals(ERTnodes.get(node), nodes.get(node));
        }
    }

    public void setup(){
        addressBook = "123.124.125.30.0.3" +
                "123.124.125.40.0.4" +
                "123.124.125.50.0.5" +
                "123.124.125.60.0.6";

        nodes.put("0.0.3", "123.124.125.3");
        nodes.put("0.0.4", "123.124.125.4");
        nodes.put("0.0.5", "123.124.125.5");
        nodes.put("0.0.6", "123.124.125.6");
    }

}
