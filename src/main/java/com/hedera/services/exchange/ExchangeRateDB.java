package com.hedera.services.exchange;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExchangeRateDB {

    private static final Logger LOGGER = LogManager.getLogger(ExchangeRateTool.class);

    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
            .withRegion(Regions.US_EAST_2)
            .build();

    public static void pushExchangeRateToDB(ExchangeRate exchangeRate ){

        try{

            LOGGER.info("Pushing Exchange rate at {} to database", exchangeRate.getNextExpirationTimeInSeconds());
            DynamoDB dynamoDB = new DynamoDB(client);

            Table table = dynamoDB.getTable("ExchangeRate");

            // use nextrate expiration as primary key and Json of the exchange rate as value in the table item.
            Item item = new Item()
                    .withPrimaryKey("ExpirationTime", exchangeRate.getNextExpirationTimeInSeconds())
                    .withString("ExchangeRateFile", exchangeRate.toString());

            table.putItem(item);
            LOGGER.info("Successfully pushed Exchange rate at {} to database", exchangeRate.getNextExpirationTimeInSeconds());

        }
        catch (Exception e){
            LOGGER.warn("Filed to push Exchange rate at {} to database", exchangeRate.getNextExpirationTimeInSeconds());
        }

    }

    public static void pushRetrievedExchangesToDB(){

    }

    public static void pushUTCMidnightRateToDB(ExchangeRate exchangeRate){
        try{

            LOGGER.info("Pushing Exchange rate at {} to database", exchangeRate.getNextExpirationTimeInSeconds());
            DynamoDB dynamoDB = new DynamoDB(client);

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
