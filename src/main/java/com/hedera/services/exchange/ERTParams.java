package com.hedera.services.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedera.hashgraph.sdk.account.AccountId;

import java.io.FileReader;
import java.util.HashMap;
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

    @JsonProperty("maxTransactionFee")
    private long maxTransactionFee;

    @JsonProperty("fileId")
    private String fileId;

    @JsonProperty("operatorId")
    private String operatorId;

    @JsonProperty("operatorKey")
    private String operatorKey;

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
