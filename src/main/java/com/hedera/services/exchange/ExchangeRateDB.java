package com.hedera.services.exchange;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExchangeRateDB {

    private static final Logger LOGGER = LogManager.getLogger(ExchangeRateTool.class);

    private static final String TABLE_NAME = "ExchangeRate";

    private static final AmazonDynamoDB CLIENT = AmazonDynamoDBClientBuilder.standard()
            .withRegion(Regions.US_EAST_1)
            .build();

    public static void pushExchangeRateToDB(final ExchangeRate exchangeRate) {

        try{
            LOGGER.info("Pushing Exchange rate at {} to database", exchangeRate.getNextExpirationTimeInSeconds());

            createTableIfNotExists(TABLE_NAME, CLIENT);
            waitUntilTableIsActive(TABLE_NAME, CLIENT);

            final DynamoDB dynamoDB = new DynamoDB(CLIENT);
            final Table table = dynamoDB.getTable("ExchangeRate");

            final Item item = new Item()
                    .withPrimaryKey("ExpirationTime", exchangeRate.getNextExpirationTimeInSeconds())
                    .withString("ExchangeRateFile", exchangeRate.toJson());

            table.putItem(item);
            LOGGER.info("Successfully pushed Exchange rate at {} to database", exchangeRate.getNextExpirationTimeInSeconds());

        } catch (final Exception ex){
            throw new RuntimeException("Failed to push Exchange rate to database", ex);
        }

    }

    private static void waitUntilTableIsActive(final String tableName, final AmazonDynamoDB dynamoDB) throws InterruptedException {
        TableUtils.waitUntilActive(dynamoDB, tableName);
    }

    private static void createTableIfNotExists(final String tableName, final AmazonDynamoDB dynamoDB) {
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        final KeySchemaElement expirationTimeElement = new KeySchemaElement()
                .withAttributeName("ExpirationTime")
                .withKeyType(KeyType.RANGE);

        final KeySchemaElement exchangeRateFileElement = new KeySchemaElement()
                .withAttributeName("ExchangeRateFile")
                .withKeyType(KeyType.HASH);

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

    public static void pushRetrievedExchangesToDB(){

    }

    public static void pushUTCMidnightRateToDB(ExchangeRate exchangeRate){
        try{

            LOGGER.info("Pushing Exchange rate at {} to database", exchangeRate.getNextExpirationTimeInSeconds());
            DynamoDB dynamoDB = new DynamoDB(CLIENT);

            Table table = dynamoDB.getTable("MidnightUTCRates");

            // use nextrate expiration as primary key and Json of the exchange rate as value in the table item.
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
/*
    public ExchangeRate getExchangeRateToValidate(String lastUTCMidnightTime){
        try{
            LOGGER.info("get Exchange rate from database");
            DynamoDB dynamoDB = new DynamoDB(client);

            Table table = dynamoDB.getTable("MidnightUTCRates");
            QuerySpec spec = new QuerySpec()
                    .withKeyConditionExpression(String.format("ExpirationTime = {}", lastUTCMidnightTime));

            ItemCollection<QueryOutcome> items = table.query(spec);

            Iterator<Item> iterator = items.iterator();
            Item item = null;
            while (iterator.hasNext()) {
                item = iterator.next();
                System.out.println(item.toJSONPretty());
                // TODO get an object mapper and cast it to ExchangeRate object.

            }

            LOGGER.info("Successfully retrieved last Midnight Exchange rate from database");

        }
        catch (Exception e){
            LOGGER.warn("Failed to retrieve the last midnight exchange rate ");
        }
    }

 */
}
