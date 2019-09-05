package com.hedera.services.exchange;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.services.exchange.database.AWSDBParams;
import com.hedera.services.exchange.database.ExchangeDB;
import com.hedera.services.exchange.database.ExchangeRateAWSRD;
import com.hedera.services.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class reads the parameters from the config file
 */

public class ERTParams {

    private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false);

    @JsonProperty("exchanges")
    private Map<String, String> exchanges;

    @JsonProperty("maxDelta")
    private double maxDelta;

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

        if (configurationPath.contains("storage.cloud.google.com/")) {
            return readConfigFromGCP(configurationPath);
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
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Reading configuration from S3 bucket: {} and key {}", bucketName, key);
        final AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();

        try (final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucketName, key))) {
            final ERTParams ertParams = OBJECT_MAPPER.readValue(fullObject.getObjectContent(), ERTParams.class);
            return ertParams;
        }
    }

    private static ERTParams readConfigFromGCP(final String endPoint) throws IOException {
        final String[] gcsParams = endPoint.split("/");
        if (gcsParams.length < 3) {
            throw new IllegalArgumentException("Not enough parameters to read from Google Cloud Storage: " + endPoint);
        }

        final String fileNameWithParameters = gcsParams[gcsParams.length - 1];
        final String fileName = fileNameWithParameters.split("\\?")[0];
        final String bucketName = gcsParams[gcsParams.length - 2];
        return readConfigFromGCP(bucketName, fileName);
    }

    private static ERTParams readConfigFromGCP(final String bucketName, final String srcFileName) throws IOException {
        final Storage storage = StorageOptions.getDefaultInstance().getService();
        final Blob blob = storage.get(BlobId.of(bucketName, srcFileName));
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blob.downloadTo(outputStream);
        return OBJECT_MAPPER.readValue(outputStream.toByteArray(), ERTParams.class);
    }

    public static ERTParams readConfig(final String configFilePath) {

        LOGGER.info(Exchange.EXCHANGE_FILTER, "Reading config from {}", configFilePath);

        try (final FileReader configFile = new FileReader(configFilePath)){
            final ERTParams ertParams = OBJECT_MAPPER.readValue(configFile, ERTParams.class);
            LOGGER.info(Exchange.EXCHANGE_FILTER, "Config file is read successfully");
            return ertParams;
        } catch (final FileNotFoundException ex) {
            final String exceptionMessage = String.format("Reading config from %s failed. File is not found.", configFilePath);
            LOGGER.error(Exchange.EXCHANGE_FILTER, exceptionMessage);
            throw new IllegalArgumentException(exceptionMessage, ex);
        } catch (final IOException ex) {
            final String exceptionMessage = String.format("Failed to load configuration %s", configFilePath);
            LOGGER.error(Exchange.EXCHANGE_FILTER, exceptionMessage);
            throw new RuntimeException(exceptionMessage);
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
        return ExchangeRateUtils.getDecryptedEnvironmentVariableFromAWS("OPERATOR_KEY");
    }

    public Rate getDefaultRate() {
        return new Rate(this.defaultHbarEquiv, this.defaultCentEquiv, this.getCurrentExpirationTime());
    }

    private long getCurrentExpirationTime() {
        final long currentTime = System.currentTimeMillis();
        final long currentHourOnTheDot = currentTime - (currentTime % 3_600_000);
        final long currentExpirationTime = currentHourOnTheDot + 3_600_000;
        return currentExpirationTime;
    }

    public ExchangeDB getExchangeDB() {
        return new ExchangeRateAWSRD(new AWSDBParams());
    }
}
