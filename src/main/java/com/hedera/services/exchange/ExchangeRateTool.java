package com.hedera.services.exchange;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HederaException;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.crypto.Key;
import com.hedera.hashgraph.sdk.file.FileId;
import com.hedera.hashgraph.sdk.file.FileUpdateTransaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ExchangeRateTool {

    // logger object to write logs into
    private static final Logger log = LogManager.getLogger(ExchangeRateTool.class);

    // member variables
    private String privateKey;
    private List<String> exchangeAPIList;
    private String mainNetAPI;
    private String pricingDBAPI;
    private Double maxDelta;
    private Double erNow;
    private Double tE;
    private Double tO;
    private String hederaFileIdentifier;
    private Double frequency;

    public static void main(String args[]) throws HederaException {
        // TODO : read the config file and save the parameters.

        // using the frequency read from the config file, spawn a thread that does the functions.
     //   ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
     //   service.scheduleAtFixedRate( new ERTproc(privateKey, exchangeAPIList, mainNetAPI,
     //   pricingDBAPI, maxDelta, prevMedian, currMedian, hederaFileIdentifier),
     //           0, frequency, Timer.Seconds);

        // we wait a while for the thread to finish executing and fetch the details the ERTproc writes to the
        // database and update prev and curr medians so that we can send them to the new thread.

        final ERTproc proc = new ERTproc("0",
                null,
                "0",
                "0",
                0.0,
                0.0,
                0l,
                "0");
        final ExchangeRate exchangeRate = proc.call();

        final AccountId accountId = new AccountId(3);
        final String address = "0.testnet.hedera.com:50211";
        final Map<AccountId, String> nodes = new HashMap<>();
        nodes.put(accountId, address);

        final Key operatorKey = null;
        final Client client = new Client(nodes).setMaxTransactionFee(100_000_000_000L);
        final TransactionId transaction = new FileUpdateTransaction(client)
                .setFileId(FileId.fromString("0.0.112"))
                .setContents(exchangeRate.toExchangeRateSet().toByteArray())
                .addKey(operatorKey)
                .execute();

        final Instant validStart = transaction.getValidStart();
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public List<String> getExchangeAPIList() {
        return exchangeAPIList;
    }

    public void setExchangeAPIList(List<String> exchangeAPIList) {
        this.exchangeAPIList = exchangeAPIList;
    }

    public String getMainNetAPI() {
        return mainNetAPI;
    }

    public void setMainNetAPI(String mainNetAPI) {
        this.mainNetAPI = mainNetAPI;
    }

    public String getPricingDBAPI() {
        return pricingDBAPI;
    }

    public void setPricingDBAPI(String pricingDBAPI) {
        this.pricingDBAPI = pricingDBAPI;
    }

    public Double getMaxDelta() {
        return maxDelta;
    }

    public void setMaxDelta(Double maxDelta) {
        this.maxDelta = maxDelta;
    }

    public Double getErNow() {
        return erNow;
    }

    public void setErNow(Double erNow) {
        this.erNow = erNow;
    }

    public Double gettE() {
        return tE;
    }

    public void settE(Double tE) {
        this.tE = tE;
    }

    public String getHederaFileIdentifier() {
        return hederaFileIdentifier;
    }

    public void setHederaFileIdentifier(String hederaFileIdentifier) {
        this.hederaFileIdentifier = hederaFileIdentifier;
    }

    public Double getFrequency() {
        return frequency;
    }

    public void setFrequency(Double frequency) {
        this.frequency = frequency;
    }

    public Double gettO() {
        return tO;
    }

    public void settO(Double tO) {
        this.tO = tO;
    }
}
