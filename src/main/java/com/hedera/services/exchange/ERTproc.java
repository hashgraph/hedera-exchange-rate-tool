package com.hedera.services.exchange;

import com.hedera.services.exchange.database.ExchangeDB;
import com.hedera.services.exchange.exchanges.Bitrex;
import com.hedera.services.exchange.exchanges.Coinbase;
import com.hedera.services.exchange.exchanges.Exchange;
import com.hedera.services.exchange.exchanges.Liquid;
import com.hedera.services.exchange.exchanges.UpBit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;

/**
 * This class implements the methods that we perform periodically to generate Exchange rate
 */
public class ERTproc {

    private static final Logger LOGGER = LogManager.getLogger(ERTproc.class);

    private static final Map<String, Function<String, Exchange>> EXCHANGES = new HashMap<>();

    static {
        EXCHANGES.put("bitrex", Bitrex::load);
        EXCHANGES.put("liquid", Liquid::load);
        EXCHANGES.put("coinbase", Coinbase::load);
        EXCHANGES.put("upbit", UpBit::load);
    }

    private final Map<String, String> exchangeApis;
    private final double maxDelta;
    private Rate midnightExchangeRate;
    private final int hbarEquiv;
    private ExchangeDB exchangeDB;

    public ERTproc(final int hbarEquiv,
            final Map<String, String> exchangeApis,
            final double maxDelta,Rate midnightExchangeRate, ExchangeDB exchangeDB) {
        this.hbarEquiv = hbarEquiv;
        this.exchangeApis = exchangeApis;
        this.maxDelta = maxDelta;
        this.midnightExchangeRate = midnightExchangeRate;
        this.exchangeDB = exchangeDB;
    }

    public ExchangeRate call() {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Start of ERT Logic");

        try {
            LOGGER.info(Exchange.EXCHANGE_FILTER, "Generating exchange objects");
            final List<Exchange> exchanges = generateExchanges();

            Double medianExRate = calculateMedianRate(exchanges);
            LOGGER.debug(Exchange.EXCHANGE_FILTER, "Median calculated : " + medianExRate);
            if (medianExRate == null){
                LOGGER.warn(Exchange.EXCHANGE_FILTER, "invalid median calculated : " + medianExRate);
                return null;
            }

            Rate currentExchangeRate = getCurrentRate(exchangeDB);
            if(currentExchangeRate == null ){
                LOGGER.debug(Exchange.EXCHANGE_FILTER, "No current rate in db, must be the first run");
                currentExchangeRate = new Rate();
            }

            final long nextExpirationTimeInSeconds = currentExchangeRate.getExpirationTimeInSeconds() + 3_600;
            Rate nextRate = new Rate(this.hbarEquiv, medianExRate, nextExpirationTimeInSeconds);

            if(midnightExchangeRate != null){
                LOGGER.debug(Exchange.EXCHANGE_FILTER, "last midnight value present .. validating the median");
                if (!midnightExchangeRate.isValid(maxDelta, nextRate)){
                    if (this.midnightExchangeRate.compareTo(medianExRate) > 0) {
                        medianExRate = this.midnightExchangeRate.getMinExchangeRate(maxDelta);
                    } else {
                        medianExRate = this.midnightExchangeRate.getMaxExchangeRate(maxDelta);
                    }

                    nextRate = new Rate(this.hbarEquiv, medianExRate, nextExpirationTimeInSeconds);
                }
            }
            else {
                LOGGER.debug(Exchange.EXCHANGE_FILTER, "No midnight value found. " +
                        "skipping validation of the calculated median");

            }

            return new ExchangeRate(currentExchangeRate, nextRate);
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Double calculateMedianRate(final List<Exchange> exchanges) {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Computing median");

        exchanges.removeIf(x -> x == null || x.getHBarValue() == null || x.getHBarValue() == 0.0);

        if (exchanges.size() == 0){
            LOGGER.error(Exchange.EXCHANGE_FILTER, "No valid exchange rates retrieved.");
            return null;
        }

        exchanges.sort(Comparator.comparingDouble(Exchange::getHBarValue));
        LOGGER.info(Exchange.EXCHANGE_FILTER, "find the median");
        if (exchanges.size() % 2 == 0 ) {
            return (exchanges.get(exchanges.size() / 2).getHBarValue() + exchanges.get(exchanges.size() / 2 - 1).getHBarValue()) / 2;
        }
        else {
            return exchanges.get(exchanges.size() / 2).getHBarValue();
        }
    }

    private List<Exchange> generateExchanges() {
        final List<Exchange> exchanges = new ArrayList<>();

        for (final Map.Entry<String, String> api : this.exchangeApis.entrySet()) {
            final Function<String, Exchange> exchangeLoader = EXCHANGES.get(api.getKey());
            if (exchangeLoader == null) {
                LOGGER.error(Exchange.EXCHANGE_FILTER,"API {} not found", api.getKey());
                continue;
            }

            final String endpoint = api.getValue();
            final Exchange exchange = exchangeLoader.apply(endpoint);
            if (exchange == null) {
                LOGGER.error(Exchange.EXCHANGE_FILTER,"API {} not loaded", api.getKey());
                continue;
            }

            exchanges.add(exchange);
        }

        return exchanges;
    }

    public static Map<String, Function<String, Exchange>> getEXCHANGES() {
        return EXCHANGES;
    }

    private static Rate getCurrentRate(final ExchangeDB exchangeDb) throws Exception {
        ExchangeRate exchangeRate = exchangeDb.getLatestExchangeRate();
        if (exchangeRate != null) {
            LOGGER.info(Exchange.EXCHANGE_FILTER, "Using latest exchange rate as current exchange rate");
            return exchangeRate.getNextRate();
        }
        return null;
    }
}
