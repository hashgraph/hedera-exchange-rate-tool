package com.hedera.services.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);

    private static final ERTParams DEFAULT = new ERTParams();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false);

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
    private long frequencyInSeconds;

    public static ERTParams readConfig(String configFilePath) {

        LOGGER.log(Level.INFO, "Reading config from {}", configFilePath);

        try {
            FileReader configFile = new FileReader(configFilePath);
            final ERTParams ertParams = OBJECT_MAPPER.readValue(configFile, ERTParams.class);

            LOGGER.log(Level.INFO, "Config file is read successfully");

            return ertParams;
        }
        catch (FileNotFoundException e ) {
            LOGGER.log(Level.ERROR, "Reading config from {} failed. FIle is not found ", configFilePath);
            return DEFAULT;
        }
        catch (Exception e){
            LOGGER.log(Level.ERROR, "Mapping error : {}", e.getMessage());
            return DEFAULT;
        }
    }

    public String toJson() throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(this);
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

    public long getFrequencyInSeconds() {
        return frequencyInSeconds;
    }

    public void setFrequencyInSeconds(long frequencyInSeconds) {
        this.frequencyInSeconds = frequencyInSeconds;
    }
}
