package com.hedera.services.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * This class reads the parameters from the config file
 */

public class ERTParams {

    @JsonProperty("exchanges")
    private Map<String, String> exchanges;

    @JsonProperty("maxDelta")
    private double maxDelta;

    @JsonProperty("privateKeyPath")
    private String privateKeyPath;

    @JsonProperty("Nodes")
    private Map<String, String> nodes;

    @JsonProperty("payAccount")
    private String payAccount;

    @JsonProperty("fileIdentifier")
    private String fileIdentifier;

    @JsonProperty("frequencyInSeconds")
    private String frequencyInSeconds;

    public static ERTParams readConfig() throws Exception {

        ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);

        FileReader configFile = new FileReader("src/main/resources/config.json");
        final ERTParams ertParams = OBJECT_MAPPER.readValue(configFile, ERTParams.class);

        return ertParams;
    }


    public Map<String, String> getExchangeAPIList() {
        return exchanges;
    }

    public void setExchangeAPIList(Map<String, String> exchangeAPIList) {
        this.exchanges = exchangeAPIList;
    }

    public double getMaxDelta() {
        return maxDelta;
    }

    public void setMaxDelta(double maxDelta) {
        this.maxDelta = maxDelta;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public Map<String, String> getNetworkNodes() {
        return nodes;
    }

    public void setNetworkNodes(Map<String, String> nodes) {
        this.nodes = nodes;
    }

    public String getPayAccount() {
        return payAccount;
    }

    public void setPayAccount(String payAccount) {
        this.payAccount = payAccount;
    }

    public String getFileIdentifier() {
        return fileIdentifier;
    }

    public void setFileIdentifier(String fileIdentifier) {
        this.fileIdentifier = fileIdentifier;
    }

    public String getFrequencyInSeconds() {
        return frequencyInSeconds;
    }

    public void setFrequencyInSeconds(String frequencyInSeconds) {
        this.frequencyInSeconds = frequencyInSeconds;
    }
}
