package com.hedera.exchange;

import com.hedera.hashgraph.sdk.Client;

public class HederaNetwork {

    private String networkName;
    private Client hederaClient;

    public HederaNetwork(String networkName, Client hederaClient) {
        this.networkName = networkName;
        this.hederaClient = hederaClient;
    }

    public String getNetworkName() {
        return networkName;
    }

    public Client getHederaClient() {
        return hederaClient;
    }

}
