package com.hedera.services.exchange;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.file.FileContentsQuery;
import com.hedera.hashgraph.sdk.file.FileId;
import com.hedera.hashgraph.sdk.file.FileUpdateTransaction;
import com.hedera.hashgraph.sdk.proto.FileGetContentsResponse;
import com.hedera.services.exchange.database.DynamoDBExchangeRate;
import com.hedera.services.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.Arrays;

public class ExchangeRateTool {

    private static final String UPDATE_ERROR_MESSAGE = "The Exchange Rates weren't updated successfully";

    private static final Logger LOGGER = LogManager.getLogger(ExchangeRateTool.class);

    public static void main(final String ... args) throws Exception {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Starting ExchangeRateTool");

        final ERTParams params = ERTParams.readConfig(args);


        Rate currentRate = params.getDefaultRate();
        final ERTproc proc = new ERTproc(params.getDefaultHbarEquiv(),
                params.getExchangeAPIList(),
                params.getMaxDelta(),
                currentRate);

        final ExchangeRate exchangeRate = proc.call();
        final byte[] exchangeRateAsBytes = exchangeRate.toExchangeRateSet().toByteArray();
        final AccountId operatorId = AccountId.fromString(params.getOperatorId());

        final FileId fileId = FileId.fromString(params.getFileId());

        final Ed25519PrivateKey privateOperatorKey =  Ed25519PrivateKey.fromString(params.getOperatorKey());
        final Client client = new Client(params.getNodes())
                .setMaxTransactionFee(params.getMaxTransactionFee())
                .setOperator(operatorId, privateOperatorKey);
        
        final FileUpdateTransaction fileUpdateTransaction = new FileUpdateTransaction(client)
                .setFileId(fileId)
                .setContents(exchangeRateAsBytes)
                .addKey(privateOperatorKey.getPublicKey());

        fileUpdateTransaction.execute();

        final FileGetContentsResponse contentsResponse = new FileContentsQuery(client).setFileId(fileId).execute();
        final long costPerCheck = contentsResponse.getHeader().getCost();
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Cost to validate file contents is {}", costPerCheck);
        final byte[] contentsRetrieved = contentsResponse.getFileContents().getContents().toByteArray();
        if (Arrays.areEqual(exchangeRateAsBytes, contentsRetrieved)) {
            LOGGER.error(Exchange.EXCHANGE_FILTER, UPDATE_ERROR_MESSAGE);
            throw new RuntimeException(UPDATE_ERROR_MESSAGE);
        }

        if(exchangeRate.isMidnightTime()){
            DynamoDBExchangeRate.pushUTCMidnightRateToDB(exchangeRate);
        }
        DynamoDBExchangeRate.pushExchangeRateToDB(exchangeRate);
        DynamoDBExchangeRate.pushRetrievedExchangesToDB(exchangeRate);
        LOGGER.info(Exchange.EXCHANGE_FILTER, "The Exchange Rates were successfully updated");
    }
}
