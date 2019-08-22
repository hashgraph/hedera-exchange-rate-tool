package com.hedera.services.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedera.hashgraph.sdk.account.AccountId;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
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

    @JsonProperty("maxTransactionFee")
    private long maxTransactionFee;

    @JsonProperty("fileId")
    private String fileId;

    @JsonProperty("operatorId")
    private String operatorId;

    @JsonProperty("operatorKey")
    private String operatorKey;

    public static ERTParams readConfig(String configFilePath) {

        LOGGER.info("Reading config from {}", configFilePath);

        try {
            FileReader configFile = new FileReader(configFilePath);
            final ERTParams ertParams = OBJECT_MAPPER.readValue(configFile, ERTParams.class);

            LOGGER.debug("Config file is read successfully");

            return ertParams;
        }
        catch (final FileNotFoundException e ) {
            LOGGER.error("Reading config from {} failed. FIle is not found ", configFilePath);
            return DEFAULT;
        }
        catch (final Exception e){
            LOGGER.error("Config file Mapping error : {}", e.getMessage());
            return DEFAULT;
        }
    }

    public String toJson() throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(this);
    }

    public Map<String, String> getExchangeAPIList() {
        return exchanges;
    }

    public double getMaxDelta() {
        return maxDelta;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public Map<String, String> getNetworkNodes() {
        return nodes;
    }

    public Map<AccountId, String> getNodes() {
        final Map<AccountId, String> accountToNodeAddresses = new HashMap<>();
        for (final Map.Entry<String, String> node : this.nodes.entrySet()) {
            final AccountId nodeId = AccountId.fromString(node.getKey());
            accountToNodeAddresses.put(nodeId, node.getValue());
        }

        return accountToNodeAddresses;
    }

    public String getPayAccount() {
        return payAccount;
    }

    public long getMaxTransactionFee() {
        return this.maxTransactionFee;
    }

    public String getFileId() {
        return this.fileId;
    }

    public String getOperatorId() {
        return this.operatorId;
    }

    public String getOperatorKey() {
        return this.operatorKey;
    }
}
