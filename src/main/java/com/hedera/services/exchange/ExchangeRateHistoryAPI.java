package com.hedera.services.exchange;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.hedera.services.exchange.database.AWSDBParams;
import com.hedera.services.exchange.database.ExchangeDB;
import com.hedera.services.exchange.database.ExchangeRateAWSRD;
import com.hedera.services.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class implements an API which returns the data from the last 'n'[defaulted to 5] successful runs of ERT
 *
 * @author anighanta
 */
public class ExchangeRateHistoryAPI implements RequestStreamHandler {

    private static Map<String, String> HEADERS = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger(ExchangeRateHistoryAPI.class);
    private static int NO_OF_RECORDS = 5;
    private final static long BOUND = 25;
    private final static long SECONDS_IN_HOUR = 3600;
    private final static long SECONDS_IN_DAY = 86400;
    private static DateFormat UTC_DATETIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    static {
        HEADERS.put("Access-Control-Allow-Origin", "*");
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

        JSONParser requestParser = new JSONParser();
        BufferedReader requestReader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        responseJson.put("headers", HEADERS);
        int no_of_records = NO_OF_RECORDS;
        UTC_DATETIME_FORMAT.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));

        try{

            JSONObject requestEvent = (JSONObject) requestParser.parse(requestReader);

            if(requestEvent.get("queryStringParameters") != null){
                JSONObject queryStringParameters = (JSONObject) requestEvent.get("queryStringParameters");
                if (queryStringParameters.get("no_of_records") != null) {
                    no_of_records = Integer.parseInt((String) queryStringParameters.get("no_of_records"));
                }
            }

            final ExchangeDB exchangeDb = new ExchangeRateAWSRD(new AWSDBParams());
            LOGGER.info(Exchange.EXCHANGE_FILTER, "params received : {}", no_of_records);
            NO_OF_RECORDS = no_of_records;
            ExchangeRate midnightRate = exchangeDb.getLatestMidnightExchangeRate();
            long currMidnightTime = midnightRate.getNextExpirationTimeInSeconds();
            LOGGER.info(Exchange.EXCHANGE_FILTER, "current Midnight time : {}", currMidnightTime);
            LOGGER.info(Exchange.EXCHANGE_FILTER, "current midnight rate : {}", midnightRate.toJson());
            String latestQueriedRate = exchangeDb.getLatestQueriedRate();
            ExchangeRate latestExchangeRate = exchangeDb.getLatestExchangeRate();

            long latestExpirationTime = latestExchangeRate.getNextExpirationTimeInSeconds();

            List<ExchangeRateHistory> results = new ArrayList<>();

            results.add(new ExchangeRateHistory(toDate(latestExpirationTime),
                            latestQueriedRate,
                            calulateMedian(latestExchangeRate),
                            isSmoothed(midnightRate.getNextRate(),
                                    latestExchangeRate.getNextRate()),
                            midnightRate,
                            latestExchangeRate.getCurrentRate(),
                            latestExchangeRate.getNextRate()
                    )
            );

            long expirationTime = latestExpirationTime;

            for (int i = 1; i < NO_OF_RECORDS; i++) {
                expirationTime -= SECONDS_IN_HOUR;
                //pull the appropriate midnight rate
                if(expirationTime <= currMidnightTime){
                    LOGGER.info(Exchange.EXCHANGE_FILTER, "day changed. fetching older midnight rate");
                    currMidnightTime -= SECONDS_IN_DAY;
                    midnightRate = exchangeDb.getMidnightExchangeRate(currMidnightTime);
                    LOGGER.info(Exchange.EXCHANGE_FILTER, "adjusted current Midnight time : {}", currMidnightTime);
                    LOGGER.info(Exchange.EXCHANGE_FILTER, "adjusted current midnight rate : {}", midnightRate.toJson());
                }

                ExchangeRate currentExchangeRate = exchangeDb.getExchangeRate(expirationTime);
                String cuuQueriedRate = exchangeDb.getQueriedRate(expirationTime);

                results.add(new ExchangeRateHistory(toDate(expirationTime),
                        cuuQueriedRate,
                        calulateMedian(currentExchangeRate),
                        isSmoothed(midnightRate.getNextRate(),
                                currentExchangeRate.getNextRate()),
                        midnightRate,
                        currentExchangeRate.getCurrentRate(),
                        currentExchangeRate.getNextRate()
                        )
                );
            }
            String result = "";
            for (ExchangeRateHistory ERH : results) {
                result += ERH.toJson() + ",";
            }

            result = result.replaceAll("\\],\\[", ",");
            result = result.substring(0, result.length() - 1);

            responseJson.put("statusCode", 200);
            responseJson.put("body", result);
        }
        catch (Exception e){
            LOGGER.error(Exchange.EXCHANGE_FILTER, e.getMessage());
            responseJson.put("statusCode", 400);
            responseJson.put("body", e.getMessage());
        }

        OutputStreamWriter responseWriter = new OutputStreamWriter(outputStream, "UTF-8");
        responseWriter.write(responseJson.toString());
        responseWriter.close();
    }

    private static double calulateMedian(ExchangeRate exchangeRate){
        LOGGER.info(Exchange.EXCHANGE_FILTER, "calculating median");
        double median = ((double) exchangeRate.getNextRate().getCentEquiv() / exchangeRate.getNextRate().getHBarEquiv()) / 100 ;

        BigDecimal medianBD = new BigDecimal(Double.toString(median));
        medianBD = medianBD.setScale(5, RoundingMode.HALF_UP);
        return medianBD.doubleValue();
    }

    private static String toDate(long expirationTime){
        LOGGER.info(Exchange.EXCHANGE_FILTER, "converting epoc to utc date time format");
        Date date = new Date(expirationTime*1000);
        return UTC_DATETIME_FORMAT.format(date);
    }

    private boolean isSmoothed(Rate midnightRate, Rate nextRate)
    {
        return !midnightRate.isSmallChange(BOUND, nextRate);
    }
}
