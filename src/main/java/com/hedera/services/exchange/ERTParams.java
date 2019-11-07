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
import java.util.Set;

/**
 * This class reads the parameters from the config file and provides get methods to fetch the configuration parameters.
 *
 * @author Anirudh, Cesar
 */
public class ERTParams {

    private static final Logger LOGGER = LogManager.getLogger(ERTParams.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false);

    @JsonProperty("exchanges")
    private Map<String, String> exchanges;

    @JsonProperty("exchangeRateAllowedPercentage")
    private long bound;

    @JsonProperty("Nodes")
    private Map<String, String> nodes;

    @JsonProperty("payerAccount")
    private String payAccount;

    @JsonProperty("frequencyInSeconds")
    private long frequencyInSeconds;

    @JsonProperty("maxTransactionFee")
    private long maxTransactionFee;

    @JsonProperty("floorCentsPerHbar")
    private long floor;

    @JsonProperty("fileId")
    private String fileId;

    @JsonProperty("operatorId")
    private String operatorId;

    @JsonProperty("defaultCentEquiv")
    private int defaultCentEquiv;

    @JsonProperty("defaultHbarEquiv")
    private int defaultHbarEquiv;

    @JsonProperty("validationDelayInMilliseconds")
    private int validationDelayInMilliseconds;

    /**
     * Return a ERTParams class populated with the configuration parameters read from the config file.
     * The reading method depends on the argument/config file path passed in the call.
     *
     *  Possible config file paths:
     *  Amazon s3
     *  Google cloud storage
     *  local path
     *
     * @param args
     * @return ERTParams object
     * @throws IOException
     */
    public static ERTParams readConfig(final String[]  args) throws IOException {
        if (args == null || args.length == 0) {
            return readDefaultConfig();
        }

        final String configurationPath = args[0];
        if (configurationPath == null || configurationPath.trim().length() < 1) {
            return readDefaultConfig();
        }

        LOGGER.info("Using configuration file: {}", configurationPath);

        if (configurationPath.contains("s3.amazonaws.com/")) {
            return readConfigFromAWSS3(configurationPath);
        }

        if (configurationPath.contains("storage.cloud.google.com/")) {
            return readConfigFromGCP(configurationPath);
        }

        if (configurationPath.contains("TO_DECIDE:AWS_NodeAddressFormat")){
            return readDefaultConfig(configurationPath);
        }

        return readConfig(configurationPath);
    }

    /**
     * Reads the AWS instance address from the arguments and replaces the node address in the
     * default configuration
     * @param awsInstanceAddress
     * @return ERTParams object
     */
    private static ERTParams readDefaultConfig(String awsInstanceAddress) throws IOException {
        final String defaultConfigUri = ExchangeRateUtils.getDecryptedEnvironmentVariableFromAWS("DEFAULT_CONFIG_URI");
        ERTParams ertParams = readConfigFromAWSS3(defaultConfigUri);

        Set<String> nodeNames = ertParams.nodes.keySet();
        for(String nodeName : nodeNames){
            ertParams.nodes.put(nodeName, awsInstanceAddress);
        }
        return ertParams;
    }

    /**
     * Read default config from amazon s3 if no config file path is provided.
     * @return ERTParams object
     * @throws IOException
     */
    private static ERTParams readDefaultConfig() throws IOException {
        final String defaultConfigUri = ExchangeRateUtils.getDecryptedEnvironmentVariableFromAWS("DEFAULT_CONFIG_URI");
        return readConfigFromAWSS3(defaultConfigUri);
    }

    /**
     * Read Config file from the Amazon S3 bucket
     * @param endpoint - url for the config file in the amazon s3 bucket.
     * @return ERTParams object
     * @throws IOException
     */
    private static ERTParams readConfigFromAWSS3(final String endpoint) throws IOException {
        LOGGER.info("Reading configuration file from AWS S3: {}", endpoint);
        final String[] s3Params = endpoint.split("/");
        if (s3Params.length < 3) {
            throw new IllegalArgumentException("Not enough parameters to read from S3: " + endpoint);
        }

        final String key = s3Params[s3Params.length - 1];
        final String bucketName = s3Params[s3Params.length - 2];
        return readConfigFromAWSS3(bucketName, key);
    }

    /**
     * Helper method to read config file from the s3 bucket once you have the url to the file in the s3 bucket.
     * @param bucketName
     * @param key
     * @return ERTParams object
     * @throws IOException
     */
    private static ERTParams readConfigFromAWSS3(final String bucketName, final String key) throws IOException {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Reading configuration from S3 bucket: {} and key {}", bucketName, key);
        final AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();

        try (final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucketName, key))) {
            final ERTParams ertParams = OBJECT_MAPPER.readValue(fullObject.getObjectContent(), ERTParams.class);
            return ertParams;
        }
    }

    /**
     * Read config file from Google cloud storage
     * @param endPoint - url for the config file in Google Cloud Storage
     * @return ERTParams object
     * @throws IOException
     */
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

    /**
     * Helper method to read config file from the Google Cloud storage
     * @param bucketName
     * @param srcFileName
     * @return ERTParams object
     * @throws IOException
     */
    private static ERTParams readConfigFromGCP(final String bucketName, final String srcFileName) throws IOException {
        final Storage storage = StorageOptions.getDefaultInstance().getService();
        final Blob blob = storage.get(BlobId.of(bucketName, srcFileName));
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blob.downloadTo(outputStream);
        return OBJECT_MAPPER.readValue(outputStream.toByteArray(), ERTParams.class);
    }

    /**
     * Read the config file from a local path.
     * @param configFilePath
     * @return ERTParams object
     */
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

    /**
     * Converts the ERTParams object into a Json string using OBJECT_MAPPER
     * @return Json String
     * @throws JsonProcessingException
     */
    public String toJson() throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(this);
    }

    /**
     * Returns a Map of Exchanges and their APIs to get the HBAR rates.
     * @return Map<String, String> of ExchangeName:ExchangeURL
     */
    public Map<String, String> getExchangeAPIList() {
        return exchanges;
    }

    /**
     * Return the bound
     * @return bound
     */
    public long getBound() {
        return bound;
    }

    /**
     * get the default HbarEquiv value
     * @return
     */
    public int getDefaultHbarEquiv() {
        return this.defaultHbarEquiv;
    }

    /**
     * get the frequency at which ERT should be running.
     * @return
     */
    public long getFrequencyInSeconds() {
        return this.frequencyInSeconds;
    }

    /**
     * Return the network nodes ERT is sending the ERT file update to.
     * @return Map of Node's AccountID to its IpAddress.
     */
    public Map<AccountId, String> getNodes() {
        final Map<AccountId, String> accountToNodeAddresses = new HashMap<>();
        for (final Map.Entry<String, String> node : this.nodes.entrySet()) {
            final AccountId nodeId = AccountId.fromString(node.getKey());
            accountToNodeAddresses.put(nodeId, node.getValue());
        }

        return accountToNodeAddresses;
    }

    /**
     * Get the Pay account to execute this ER file update transaction.
     * @return account ID in string
     */
    public String getPayAccount() {
        return payAccount;
    }

    /**
     * Get the max transaction fee for file update.
     * @return
     */
    public long getMaxTransactionFee() {
        return this.maxTransactionFee;
    }

    /**
     * get Exchange Rate file ID
     * @return
     */
    public String getFileId() {
        return this.fileId;
    }

    /**
     * get the operator ID - Account from which the FIle update transaction is performed
     * @return
     */
    public String getOperatorId() {
        return this.operatorId;
    }

    /**
     * get the Floor of the Exchange Rate that is allowed.
     * @return
     */
    public long getFloor(){ return this.floor; }

    @JsonIgnore
    public String getOperatorKey() {
        return ExchangeRateUtils.getDecryptedEnvironmentVariableFromAWS("OPERATOR_KEY");
    }

    /**
     * Get the default Exchange Rate
     * @return
     */
    public Rate getDefaultRate() {
        return new Rate(this.defaultHbarEquiv, this.defaultCentEquiv, this.getCurrentExpirationTime());
    }

    /**
     * get the validation Delay in Milli Seconds
     * @return
     */
    public long getValidationDelayInMilliseconds() {
        return this.validationDelayInMilliseconds;
    }

    /**
     * get the EPOC time of the end of the current hour in seconds in UTC.
     * for example, say the current date and time is October 31st 2019, 10:34 AM
     * then currentExpirationTime will be 1572537600 in UTC
     * @return
     */
    public static long getCurrentExpirationTime() {
        final long currentTime = System.currentTimeMillis();
        final long currentHourOnTheDot = currentTime - (currentTime % 3_600_000);
        final long currentExpirationTime = currentHourOnTheDot + 3_600_000;
        return currentExpirationTime / 1000;
    }

    /**
     * Get the Database class to read and write the Exchange Rate Files.
     * @return ExchangeRateDb object as we are configured with AWS POSTGRESQL for now.
     */
    public ExchangeDB getExchangeDB() {
        return new ExchangeRateAWSRD(new AWSDBParams());
    }
}
