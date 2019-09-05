package com.hedera.services.exchange.database;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;

import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hedera.services.exchange.ExchangeRate;
import com.hedera.services.exchange.ExchangeRateTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DynamoDBExchangeRate implements ExchangeDB {

    private static final Logger LOGGER = LogManager.getLogger(ExchangeRateTool.class);

    private static final String EXCHANGE_RATE_TABLE_NAME = "ExchangeRate";
    private static final String MIDNIGHT_RATE_TABLE_NAME = "MidnightRate";
    private static final String QUERIED_RATE_TABLE_NAME = "QueriedRates";

    private static final AmazonDynamoDB CLIENT = AmazonDynamoDBClientBuilder.standard()
            .withRegion(Regions.US_EAST_1)
            .build();

    @Override
    public ExchangeRate getLatestExchangeRate() {
        return null;
    }

    @Override
    public ExchangeRate getLatestMidnightExchangeRate() {
        return null;
    }

    public void pushExchangeRate(final ExchangeRate exchangeRate) throws JsonProcessingException, InterruptedException {
        LOGGER.info("Pushing Exchange rate at {} to database", exchangeRate.getNextExpirationTimeInSeconds());

        createTableIfNotExists(EXCHANGE_RATE_TABLE_NAME, CLIENT);
        waitUntilTableIsActive(EXCHANGE_RATE_TABLE_NAME, CLIENT);


        final DynamoDB dynamoDB = new DynamoDB(CLIENT);
        final Table table = dynamoDB.getTable(EXCHANGE_RATE_TABLE_NAME);

        final Item item = new Item()
                .withPrimaryKey("ExpirationTime", exchangeRate.getNextExpirationTimeInSeconds())
                .withString("ExchangeRateFile", exchangeRate.toJson());

        table.putItem(item);
        LOGGER.info("Successfully pushed Exchange rate at {} to database", exchangeRate.getNextExpirationTimeInSeconds());
    }

    {

    }

    private static void waitUntilTableIsActive(final String tableName, final AmazonDynamoDB dynamoDB) throws InterruptedException {
        TableUtils.waitUntilActive(dynamoDB, tableName);
    }

    private static void createTableIfNotExists(final String tableName, final AmazonDynamoDB dynamoDB) {
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        final KeySchemaElement expirationTimeElement = new KeySchemaElement()
                .withAttributeName("ExpirationTime")
                .withKeyType(KeyType.HASH);

        final KeySchemaElement exchangeRateFileElement = new KeySchemaElement()
                .withAttributeName("ExchangeRateFile")
                .withKeyType(KeyType.RANGE);

        request.withKeySchema(exchangeRateFileElement, expirationTimeElement);

        final AttributeDefinition expirationTime = new AttributeDefinition()
                .withAttributeName("ExpirationTime")
                .withAttributeType(ScalarAttributeType.N);

        final AttributeDefinition exchangeRateFile = new AttributeDefinition()
                .withAttributeName("ExchangeRateFile")
                .withAttributeType(ScalarAttributeType.S);

        request.withAttributeDefinitions(expirationTime, exchangeRateFile);

        request.withBillingMode(BillingMode.PAY_PER_REQUEST);

        TableUtils.createTableIfNotExists(dynamoDB, request);
    }

    @Override
    public void pushQueriedRate(long expirationTime, String queriedRate) throws Exception {
        try{

            LOGGER.info("Pushing Exchanges Data at {} to database", expirationTime);

            createTableIfNotExists(QUERIED_RATE_TABLE_NAME, CLIENT);
            waitUntilTableIsActive(QUERIED_RATE_TABLE_NAME, CLIENT);

            final DynamoDB dynamoDB = new DynamoDB(CLIENT);
            final Table table = dynamoDB.getTable(QUERIED_RATE_TABLE_NAME);

            Item item = new Item()
                    .withPrimaryKey("ExpirationTime", expirationTime)
                    .withString("ExchangeRateFile", queriedRate);

            table.putItem(item);
            LOGGER.info("Successfully pushed Exchanges Data at {} to database", expirationTime);

        }
        catch (Exception e){
            LOGGER.warn("Filed to push Exchanges Data at {} to database", expirationTime);
        }
    }

    @Override
    public void pushMidnightRate(ExchangeRate exchangeRate) throws Exception
    {
        try{

                LOGGER.info("Pushing Midnight Exchange rate at {} to database", exchangeRate.getNextExpirationTimeInSeconds());

                createTableIfNotExists(MIDNIGHT_RATE_TABLE_NAME, CLIENT);
                waitUntilTableIsActive(MIDNIGHT_RATE_TABLE_NAME, CLIENT);

                final DynamoDB dynamoDB = new DynamoDB(CLIENT);
                final Table table = dynamoDB.getTable(MIDNIGHT_RATE_TABLE_NAME);

                Item item = new Item()
                        .withPrimaryKey("ExpirationTime", exchangeRate.getNextExpirationTimeInSeconds())
                        .withString("ExchangeRateFile", exchangeRate.toString());

                table.putItem(item);
                LOGGER.info("Successfully pushed Midnight Exchange rate at {} to database", exchangeRate.getNextExpirationTimeInSeconds());

            }
            catch (Exception e){
                LOGGER.warn("Filed to push Midnight Exchange rate at {} to database", exchangeRate.getNextExpirationTimeInSeconds());
            }
    }

    public static ExchangeRate getExchangeRateToValidate(long UTCMidnightTime){
        try{
            LOGGER.info("get Exchange rate from database");

            final ObjectMapper object_Mapper = new ObjectMapper().enable(SerializationFeature.WRAP_ROOT_VALUE);

            final DynamoDB dynamoDB = new DynamoDB(CLIENT);
            final Table table = dynamoDB.getTable(MIDNIGHT_RATE_TABLE_NAME);

            GetItemSpec spec = new GetItemSpec().withPrimaryKey("ExpirationTime", UTCMidnightTime);

            LOGGER.info("Attempting to read the item");
            Item outcome = table.getItem(spec);

            final ExchangeRate exchangeRate = object_Mapper.readValue( outcome.toJSON() , ExchangeRate.class);

            LOGGER.info("Successfully retrieved last Midnight Exchange rate from database");
            return exchangeRate;

        }
        catch (Exception e){
            LOGGER.warn("Failed to retrieve the last midnight exchange rate ");
            return null;
        }
    }
}
