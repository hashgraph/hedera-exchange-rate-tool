package com.hedera.services.exchange;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionRecord;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.file.FileContentsQuery;
import com.hedera.hashgraph.sdk.file.FileId;
import com.hedera.hashgraph.sdk.file.FileUpdateTransaction;
import com.hedera.hashgraph.sdk.proto.ExchangeRateSet;
import com.hedera.hashgraph.sdk.proto.FileGetContentsResponse;
import com.hedera.hashgraph.sdk.proto.TimestampSeconds;
import com.hedera.services.exchange.database.ExchangeDB;
import com.hedera.services.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;

public class ExchangeRateTool {

    private static final String UPDATE_ERROR_MESSAGE = "The Exchange Rates weren't updated successfully";

    private static final int DEFAULT_RETRIES = 1;

    private static final Logger LOGGER = LogManager.getLogger(ExchangeRateTool.class);

    public static void main(final String ... args) {
        run(args);
    }

    private static void run(final String ... args) {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Starting ExchangeRateTool");
        final int maxRetries = DEFAULT_RETRIES;
        int currentTries = 0;
        while (currentTries <  maxRetries) {
            try {
                execute(args);
                return;
            } catch (final Exception ex) {
                ex.printStackTrace();
                currentTries++;
                LOGGER.error(Exchange.EXCHANGE_FILTER, "Failed to execute at try {}/{} with exception {}. Retrying", currentTries, maxRetries, ex);
            }
        }

        final String errorMessage = String.format("Failed to execute after %d retries", maxRetries);
        LOGGER.error(Exchange.EXCHANGE_FILTER, errorMessage);
        throw new RuntimeException(errorMessage);
    }

    private static void execute(final String ... args) throws Exception {
        final ERTParams params = ERTParams.readConfig(args);

        final ExchangeDB exchangeDb = params.getExchangeDB();

        final long frequencyInSeconds = params.getFrequencyInSeconds();

        final ExchangeRate midnightExchangeRate = exchangeDb.getLatestMidnightExchangeRate();
        final Rate midnightRate = midnightExchangeRate == null ? null : midnightExchangeRate.getNextRate();
        final Rate currentRate = getCurrentRate(exchangeDb, params);
        final ERTproc proc = new ERTproc(params.getDefaultHbarEquiv(),
                params.getExchangeAPIList(),
                params.getBound(),
                midnightRate,
                currentRate,
                frequencyInSeconds);

        final ExchangeRate exchangeRate = proc.call();
        final byte[] exchangeRateAsBytes = exchangeRate.toExchangeRateSet().toByteArray();
        final AccountId operatorId = AccountId.fromString(params.getOperatorId());

        final FileId fileId = FileId.fromString(params.getFileId());
        final Ed25519PrivateKey privateOperatorKey =  Ed25519PrivateKey.fromString(params.getOperatorKey());
        final Client client = new Client(params.getNodes())
                .setMaxTransactionFee(params.getMaxTransactionFee())
                .setOperator(operatorId, privateOperatorKey);

        final long currentBalance = client.getAccountBalance(operatorId);
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Balance before updating the file: {}", currentBalance);
        final Duration duration = Duration.ofSeconds(220);
        final Instant expirationTime = Instant.ofEpochSecond(exchangeRate.getNextExpirationTimeInSeconds());
        final FileUpdateTransaction fileUpdateTransaction = new FileUpdateTransaction(client)
                .setFileId(fileId)
                .setTransactionValidDuration(duration)
                .setTransactionFee(1_000_000)
                .setContents(exchangeRateAsBytes)
                .addKey(privateOperatorKey.getPublicKey());

        final TransactionId firstTry = fileUpdateTransaction.execute();

        LOGGER.info("Exchange rate file hash {} bytes and hash code {}",
                exchangeRateAsBytes.length,
                Arrays.hashCode(exchangeRateAsBytes));

        LOGGER.info(Exchange.EXCHANGE_FILTER, "First update has valid start {}, account {}",
                firstTry.getValidStart(),
                firstTry.getAccountId());


        final long newBalance = client.getAccountBalance(operatorId);
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Balance after updating the file: {}", newBalance);

        final FileGetContentsResponse contentsResponse = new FileContentsQuery(client).setFileId(fileId).execute();
        final long costPerCheck = contentsResponse.getHeader().getCost();
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Cost to validate file contents is {}", costPerCheck);
        final byte[] contentsRetrieved = contentsResponse.getFileContents().getContents().toByteArray();

        LOGGER.info("The contents retrieved has {} bytes and hash code {}",
                contentsRetrieved.length,
                Arrays.hashCode(contentsRetrieved));

        final ExchangeRateSet exchangeRateSet = ExchangeRateSet.parseFrom(contentsRetrieved);
        final com.hedera.hashgraph.sdk.proto.ExchangeRate currentRateInNet = exchangeRateSet.getCurrentRate();
        final com.hedera.hashgraph.sdk.proto.ExchangeRate nextRateInNet = exchangeRateSet.getNextRate();

        final TimestampSeconds timestampSeconds = nextRateInNet.getExpirationTime();
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Next expiration time in seconds: {}", timestampSeconds.getSeconds());
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Next rate ({},{})", nextRateInNet.getHbarEquiv(), nextRateInNet.getCentEquiv());


        if (!Arrays.equals(exchangeRateAsBytes, contentsRetrieved)) {
            LOGGER.error(Exchange.EXCHANGE_FILTER, UPDATE_ERROR_MESSAGE);
            throw new RuntimeException(UPDATE_ERROR_MESSAGE);
        }

        if(exchangeRate.isMidnightTime()){
            LOGGER.info(Exchange.EXCHANGE_FILTER, "This rate expires at midnight. Pushing it to the DB");
            exchangeDb.pushMidnightRate(exchangeRate);
        }

        exchangeDb.pushExchangeRate(exchangeRate);
        exchangeDb.pushQueriedRate(exchangeRate.getNextExpirationTimeInSeconds(), proc.getExchangeJson());
        LOGGER.info(Exchange.EXCHANGE_FILTER, "The Exchange Rates were successfully updated");
    }

    private static Rate getCurrentRate(final ExchangeDB exchangeDb, final ERTParams params) throws Exception {
        final ExchangeRate exchangeRate = exchangeDb.getLatestExchangeRate();
        if (exchangeRate != null) {
            LOGGER.info(Exchange.EXCHANGE_FILTER, "Using latest exchange rate as current exchange rate");
            return exchangeRate.getNextRate();
        }

        LOGGER.info(Exchange.EXCHANGE_FILTER, "Using default exchange rate as current exchange rate");
        return params.getDefaultRate();
    }
}
