package com.hedera.services.exchange;

import com.hedera.services.exchange.database.AWSDBParams;
import com.hedera.services.exchange.database.ExchangeDB;
import com.hedera.services.exchange.database.ExchangeRateAWSRD;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class implements an API which returns the data from the last 5 successful runs of ERT
 *
 * @author anighanta
 */
public class ExchangeRateHistoryAPI {

    private static Map<String, String> HEADERS = new HashMap<>();
    private final static int NO_OF_RECORDS = 5;
    private final static long BOUND = 25;

    static {
        HEADERS.put("Access-Control-Allow-Origin", "*");
    }

    public static ExchangeRateApi.LambdaResponse getHistory() throws Exception{
        final ExchangeDB exchangeDb = new ExchangeRateAWSRD(new AWSDBParams());
        final ExchangeRate midnightRate = exchangeDb.getLatestMidnightExchangeRate();
        String latestQueriedRate = exchangeDb.getLatestQueriedRate();
        ExchangeRate latestExchangeRate = exchangeDb.getLatestExchangeRate();

        long latestExpirationTime = latestExchangeRate.getNextExpirationTimeInSeconds();

        List<ExchangeRateHistory> last5Results = new ArrayList<>();

        last5Results.add(new ExchangeRateHistory(toDate(latestExpirationTime),
                                                    latestQueriedRate,
                                                    calMedian(latestExchangeRate),
                                                    midnightRate.getNextRate()
                                                            .isSmallChange(BOUND, latestExchangeRate.getNextRate()),
                                                    midnightRate,
                                                    latestExchangeRate.getCurrentRate(),
                                                    latestExchangeRate.getNextRate()
                                                )
        );
        long expirationTime = latestExpirationTime;
        // run a loop for next 4 queries to get the history
        for(int i = 1; i < NO_OF_RECORDS; i++){
            expirationTime -= 3600;
            ExchangeRate currExchangeRate = exchangeDb.getExchangeRate(expirationTime);
            String cuuQueriedRate = exchangeDb.getQueriedRate(expirationTime);

            last5Results.add(new ExchangeRateHistory(toDate(expirationTime),
                    cuuQueriedRate,
                    calMedian(currExchangeRate),
                    midnightRate.getNextRate()
                            .isSmallChange(BOUND, currExchangeRate.getNextRate()),
                    midnightRate,
                    currExchangeRate.getCurrentRate(),
                    currExchangeRate.getNextRate()
                    )
            );
        }
        String result = "";
        for( ExchangeRateHistory ERH : last5Results ){
            result += ERH.toJson() + ",";
        }

        result = result.replaceAll("\\],\\[", ",");
        result = result.substring(0, result.length() - 1);

        return new ExchangeRateApi.LambdaResponse(200, result);
    }

    private static double calMedian(ExchangeRate exchangeRate){
        return ((double) exchangeRate.getNextRate().getCentEquiv() / (double) exchangeRate.getNextRate().getHBarEquiv()) / 100 ;
    }

    private static String toDate(long expirationTime){
        Date date = new Date(expirationTime*1000);
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        return format.format(date);
    }

}
