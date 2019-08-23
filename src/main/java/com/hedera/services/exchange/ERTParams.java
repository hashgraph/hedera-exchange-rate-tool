package com.hedera.services.exchange;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.Base64;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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

    @JsonProperty("defaultCentEquiv")
    private int defaultCentEquiv;

    @JsonProperty("defaultHbarEquiv")
    private int defaultHbarEquiv;

    public static ERTParams readConfig(final String[]  args) throws IOException {
        if (args == null || args.length == 0) {
            return readConfig("src/resources/config.json");
        }

        final String configurationPath = args[0];
        LOGGER.info("Using configuration file: {}", configurationPath);

        if (configurationPath.contains("s3.amazonaws.com/")) {
            return readConfigFromAWSS3(configurationPath);
        }

        return readConfig(configurationPath);
    }

    private static ERTParams readConfigFromAWSS3(final String endpoint) throws IOException {
        final String[] s3Params = endpoint.split("/");
        if (s3Params.length < 3) {
            throw new IllegalArgumentException("Not enough parameters to read from S3: " + endpoint);
        }

        final String key = s3Params[s3Params.length - 1];
        final String bucketName = s3Params[s3Params.length - 2];
        return readConfigFromAWSS3(bucketName, key);
    }

    private static ERTParams readConfigFromAWSS3(final String bucketName, final String key) throws IOException {
        LOGGER.info("Reading configuration from S3 bucket: {} and key {}", bucketName, key);
        final AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();

        try (final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucketName, key))) {
            final ERTParams ertParams = OBJECT_MAPPER.readValue(fullObject.getObjectContent(), ERTParams.class);
            return ertParams;
        }
    }

    public static ERTParams readConfig(final String configFilePath) {

        LOGGER.log(Level.INFO, "Reading config from {}", configFilePath);

        try {
            FileReader configFile = new FileReader(configFilePath);
            final ERTParams ertParams = OBJECT_MAPPER.readValue(configFile, ERTParams.class);

            LOGGER.log(Level.INFO, "Config file is read successfully");

            return ertParams;
        }
        catch (final FileNotFoundException e ) {
            LOGGER.log(Level.ERROR, "Reading config from {} failed. FIle is not found ", configFilePath);
            return DEFAULT;
        }
        catch (final Exception e){
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

    public double getMaxDelta() {
        return maxDelta;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public Map<String, String> getNetworkNodes() {
        return nodes;
    }

    public int getDefaultHbarEquiv() {
        return this.defaultHbarEquiv;
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

    @JsonIgnore
    public String getOperatorKey() {
        final byte[] encryptedKey = Base64.decode(System.getenv("OPERATOR_KEY"));

        final AWSKMS client = AWSKMSClientBuilder.defaultClient();

        final DecryptRequest request = new DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(encryptedKey));
        final ByteBuffer plainTextKey = client.decrypt(request).getPlaintext();
        return new String(plainTextKey.array(), Charset.forName("UTF-8"));
    }

    public Rate getDefaultRate() {
        return new Rate(this.defaultHbarEquiv, this.defaultCentEquiv, this.getCurrentExpirationTime());
    }

    private long getCurrentExpirationTime() {
        final long currentTime = System.currentTimeMillis();
        return (currentTime - (currentTime % 3_600_000) ) + 3_600_000;
    }
}
