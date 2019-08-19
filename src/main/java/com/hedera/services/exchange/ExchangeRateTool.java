package com.hedera.services.exchange;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HederaException;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.file.FileContentsQuery;
import com.hedera.hashgraph.sdk.file.FileId;
import com.hedera.hashgraph.sdk.file.FileUpdateTransaction;
import com.hedera.hashgraph.sdk.proto.FileGetContentsQuery;
import com.hedera.hashgraph.sdk.proto.FileGetContentsResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.Arrays;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ExchangeRateTool {

    private static final String OPERATOR_KEY = "302e020100300506032b65700422042091132178e72057a1d7528025956fe39b0b847f200ab59b2fdd367017f3087137";

    private static final String UPDATE_ERROR_MESSAGE = "The Exchange Rates weren't updated successfully";

    // logger object to write logs into
    private static final Logger LOGGER = LogManager.getLogger(ExchangeRateTool.class);

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
        LOGGER.info("Starting ExchangeRateTool");

        final ERTproc proc = new ERTproc("0",
                null,
                "0",
                "0",
                0.0,
                0.0,
                0l,
                "0");
        final ExchangeRate exchangeRate = proc.call();
        final byte[] exchangeRateAsBytes = exchangeRate.toExchangeRateSet().toByteArray();
        final AccountId operatorId = AccountId.fromString("0.0.57");

        final AccountId nodeId = new AccountId(3);
        final String address = "0.testnet.hedera.com:50211";
        final Map<AccountId, String> nodes = new HashMap<>();
        final FileId fileId = FileId.fromString("0.0.112");
        nodes.put(nodeId, address);

        final Ed25519PrivateKey operatorKey =  Ed25519PrivateKey.fromString(OPERATOR_KEY);;
        final Client client = new Client(nodes)
                .setMaxTransactionFee(1_000_000_000L)
                .setOperator(operatorId, operatorKey);
        
        final FileUpdateTransaction fileUpdateTransaction = new FileUpdateTransaction(client)
                .setFileId(fileId)
                .setContents(exchangeRateAsBytes)
                .addKey(operatorKey.getPublicKey());

        fileUpdateTransaction.execute();

        final FileGetContentsResponse contentsResponse = new FileContentsQuery(client).setFileId(fileId).execute();

        final byte[] contentsRetrieved = contentsResponse.getFileContents().getContents().toByteArray();
        if (Arrays.areEqual(exchangeRateAsBytes, contentsRetrieved)) {
            LOGGER.error(UPDATE_ERROR_MESSAGE);
            throw new RuntimeException(UPDATE_ERROR_MESSAGE);
        }

        LOGGER.info("The Exchange Rates were successfully updated");
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
