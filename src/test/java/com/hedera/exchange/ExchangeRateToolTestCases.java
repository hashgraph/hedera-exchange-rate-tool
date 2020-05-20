package com.hedera.exchange;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.hedera.hashgraph.proto.NodeAddressBook;

public class ExchangeRateToolTestCases {
    private Map<String, String> nodes =  new HashMap<>();
    private NodeAddressBook addressBook;

    @Test
    public void verifyNodesFromAddressBook() throws IOException {
        setup();

        Map<String, String> ERTnodes = ExchangeRateTool.getNodesFromAddressBook(addressBook);
        for( String node : ERTnodes.keySet()){
            Assert.assertEquals(ERTnodes.get(node), nodes.get(node));
        }
    }

    public void setup() throws IOException {
        File addressBookFile = new File("src/test/resources/addressBook.bin");
        FileInputStream fis = new FileInputStream(addressBookFile);
        byte[] content = new byte[(int) addressBookFile.length()];
        fis.read(content);
        addressBook = NodeAddressBook.parseFrom(content);

        nodes.put("0.0.3", "35.237.182.66:50211");
        nodes.put("0.0.4", "35.245.226.22:50211");
        nodes.put("0.0.5", "34.68.9.203:50211");
        nodes.put("0.0.6", "34.83.131.197:50211");
        nodes.put("0.0.7", "34.94.236.63:50211");
        nodes.put("0.0.8", "35.203.26.115:50211");
        nodes.put("0.0.9", "34.77.3.213:50211");
        nodes.put("0.0.10", "35.197.237.44:50211");
        nodes.put("0.0.11", "35.246.250.176:50211");
        nodes.put("0.0.12", "34.90.117.105:50211");
        nodes.put("0.0.13", "35.200.57.21:50211");
        nodes.put("0.0.14", "34.92.120.143:50211");
        nodes.put("0.0.15", "34.87.47.168:50211");
    }

}
