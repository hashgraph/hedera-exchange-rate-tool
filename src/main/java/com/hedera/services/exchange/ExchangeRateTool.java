package com.hedera.services.exchange;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.account.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.file.*;
import com.hedera.services.exchange.database.ExchangeDB;
import com.hedera.services.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This Class represents the whole Exchange Rate Tool application. This is main entry point for the application.
 *
 * @author Anirudh, Cesar
 */
public class ExchangeRateTool {
    private static final Logger LOGGER = LogManager.getLogger(ExchangeRateTool.class);
    private static final String UPDATE_ERROR_MESSAGE = "The Exchange Rates were not updated successfully";
    private static final int DEFAULT_RETRIES = 4;
    private static final String ADDRESS_BOOK_FILE_ID = "0.0.101";

    private static ERTParams ertParams;
    private static ExchangeDB exchangeDB;
    private static ERTAddressBook ertAddressBook;

    public static void main(final String ... args) {
        run(args);
    }

    /**
     * This method executes the ERT logic and if an execution fails, it retries for the a fixed number of times
     * mentioned in DEFAULT_RETRIES.
     * @param args
     */
    private static void run(final String ... args) {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Starting ExchangeRateTool");
        final int maxRetries = DEFAULT_RETRIES;
        int currentTries = 0;
        try {
            ertParams = ERTParams.readConfig(args);
            exchangeDB = ertParams.getExchangeDB();
            ertAddressBook = exchangeDB.getLatestERTAddressBook();

            while (currentTries <  maxRetries) {
                execute();
                return;
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            currentTries++;
            LOGGER.error(Exchange.EXCHANGE_FILTER, "Failed to execute at try {}/{} with exception {}. Retrying", currentTries, maxRetries, ex);
        }

        final String errorMessage = String.format("Failed to execute after %d retries", maxRetries);
        LOGGER.error(Exchange.EXCHANGE_FILTER, errorMessage);
        throw new RuntimeException(errorMessage);
    }

    /**
     * This method encapsulates all the execution logic
     * - Execute ERTProc
     * - generate a transaction using the operator key, file ID, sign it with the private key
     * - perform the FIle Update transaction
     * - check if the transaction was successful
     * - Write the generated exchange rate files into the Database
     * @throws Exception
     */
    private static void execute() throws Exception {

        final Ed25519PrivateKey privateOperatorKey = Ed25519PrivateKey.fromString(ertParams.getOperatorKey());
        final AccountId operatorId = AccountId.fromString(ertParams.getOperatorId());

        Client client = new Client( ertAddressBook != null && !ertAddressBook.getNodes().isEmpty() ?
                ertAddressBook.getNodes() : ertParams.getNodes() )
                .setMaxTransactionFee(ertParams.getMaxTransactionFee())
                .setOperator(operatorId, privateOperatorKey);

        final long frequencyInSeconds = ertParams.getFrequencyInSeconds();
        final ExchangeRate midnightExchangeRate = exchangeDB.getLatestMidnightExchangeRate();
        final Rate midnightRate = midnightExchangeRate == null ? null : midnightExchangeRate.getNextRate();
        final Rate currentRate = getCurrentRate(exchangeDB, ertParams);

        final ERTproc proc = new ERTproc(ertParams.getDefaultHbarEquiv(),
                ertParams.getExchangeAPIList(),
                ertParams.getBound(),
                ertParams.getFloor(),
                midnightRate,
                currentRate,
                frequencyInSeconds);

        final ExchangeRate exchangeRate = proc.call();
        final byte[] exchangeRateAsBytes = exchangeRate.toExchangeRateSet().toByteArray();

        final String memo = String.format("currentRate : %d, nextRate : %d, midnightRate : %d",
                exchangeRate.getCurrentRate().getCentEquiv(),
                exchangeRate.getNextRate().getCentEquiv(),
                midnightRate.getCentEquiv());
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Memo for the FileUpdate tx : {}", memo);

        final FileId exchangeRateFileId = FileId.fromString(ertParams.getFileId());
        final FileId addressBookFileId = FileId.fromString(ADDRESS_BOOK_FILE_ID);

        final Hbar currentBalance = new AccountBalanceQuery()
                                                .setAccountId(operatorId)
                                                .execute(client);

        LOGGER.info(Exchange.EXCHANGE_FILTER, "Balance before the process of updating the Exchange Rate file: {}",
                currentBalance);

        ERTAddressBook newAddressBook = fetchAddressBook(client, addressBookFileId);

        updateExchangeRateFileTxnAndValidate(exchangeRate, exchangeRateFileId, exchangeRateAsBytes, client, memo, operatorId);

        final Hbar newBalance = new AccountBalanceQuery()
                .setAccountId(operatorId)
                .execute(client);

        LOGGER.info(Exchange.EXCHANGE_FILTER, "Balance after updating the Exchange Rate file: {}", newBalance);

        exchangeDB.pushExchangeRate(exchangeRate);
        if (exchangeRate.isMidnightTime()) {
            LOGGER.info(Exchange.EXCHANGE_FILTER, "This rate expires at midnight. Pushing it to the DB");
            exchangeDB.pushMidnightRate(exchangeRate);
        }
        exchangeDB.pushQueriedRate(exchangeRate.getNextExpirationTimeInSeconds(), proc.getExchangeJson());
        exchangeDB.pushERTAddressBook(exchangeRate.getNextExpirationTimeInSeconds(), newAddressBook);

        LOGGER.info(Exchange.EXCHANGE_FILTER, "The Exchange Rates were successfully updated");
    }

    private static void updateExchangeRateFileTxnAndValidate(ExchangeRate exchangeRate,
                                                             FileId exchangeRateFileId,
                                                             byte[] exchangeRateAsBytes,
                                                             Client client,
                                                             String memo,
                                                             AccountId operatorId) throws Exception {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Pushing new ExchangeRate {}", exchangeRate.toJson());
        final TransactionId exchangeRateFileUpdateTransactionId = new FileUpdateTransaction()
                .setFileId(exchangeRateFileId)
                .setContents(exchangeRateAsBytes)
                .setTransactionMemo(memo)
                .execute(client);

        LOGGER.info("Exchange rate file hash {} bytes and hash code {}",
                exchangeRateAsBytes.length,
                Arrays.hashCode(exchangeRateAsBytes));

        LOGGER.info(Exchange.EXCHANGE_FILTER, "First update has status {}",
                exchangeRateFileUpdateTransactionId.getReceipt(client).status);

        Thread.sleep(ertParams.getValidationDelayInMilliseconds());

        /**
         * commented out code:
         *
         * Code to get file info on the ERT file that we just updated.
         * Could be useful in future to debug errors.
         */

//        FileInfo exchangeRateFileInfo = new FileInfoQuery()
//                                            .setFileId(exchangeRateFileId)
//                                            .execute(client);
//
//        LOGGER.info(Exchange.EXCHANGE_FILTER, "Exchange Rate file info : exp Time {} \n fileID {} \n isDeleted {} \n" +
//                        "keys {} \n size {}",
//                exchangeRateFileInfo.expirationTime,
//                exchangeRateFileInfo.fileId,
//                exchangeRateFileInfo.isDeleted,
//                exchangeRateFileInfo.keys,
//                exchangeRateFileInfo.size);

        final byte[] contentsRetrieved =  getFileContentsQuery(client, exchangeRateFileId);

        LOGGER.info("The contents retrieved has {} bytes and hash code {}",
                contentsRetrieved.length,
                Arrays.hashCode(contentsRetrieved));
        if (!Arrays.equals(exchangeRateAsBytes, contentsRetrieved)) {
            LOGGER.error(Exchange.EXCHANGE_FILTER, UPDATE_ERROR_MESSAGE);
            throw new RuntimeException(UPDATE_ERROR_MESSAGE);
        }
    }

    private static ERTAddressBook fetchAddressBook(Client client, FileId addressBookFileId) throws Exception {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "fetching the addressbook");

        final String addressBook = new String(getFileContentsQuery(client, addressBookFileId),
                StandardCharsets.UTF_8)
                .trim();
        LOGGER.info(Exchange.EXCHANGE_FILTER, "addressbook file contents {}", addressBook);

        Map<String, String> addressBookNodes = new HashMap<>();
        if (addressBook.isEmpty()) {
            LOGGER.warn(Exchange.EXCHANGE_FILTER, "didnt find any addresses in the address book.");
        } else {
            addressBookNodes = getNodesFromAddressBook(addressBook);
        }

        ERTAddressBook newAddressBook = new ERTAddressBook();
        newAddressBook.setNodes(addressBookNodes);
        return  newAddressBook;
    }

    /**
     * This method parses the address book and generates a map of nodeIds and their Addresses.
     * @param addressBook
     * @return Map<String, String> nodeId --> IPaddress
     */
    public static Map<String, String> getNodesFromAddressBook(String addressBook) {
        Map<String, String> nodes =  new HashMap<>();
        String[] addresses = addressBook.split("\n");
        int nodeNumber = 3;
        for(String address : addresses){
            address = address.replaceAll(" ", "");
            String[] elements = address.split("0.0.");
            if(elements.length == 2) {
                String nodeId = String.format("0.0.%d", nodeNumber++);
                String nodeAddress = elements[0].trim().replaceAll("/", "");
                nodes.put(nodeId, nodeAddress+":50211");
                LOGGER.info(Exchange.EXCHANGE_FILTER, "found node {} and its address {}:50211 in addressBook",
                        nodeId, nodeAddress);
            }
        }
        return  nodes;
    }

    /**
     * Get the current Exchange Rate from the database.
     * If not found, get the default rate from the config file.
     * @param exchangeDb Database class that we are using.
     * @param params ERTParams object to read the config file.
     * @return Rate object
     * @throws Exception
     */
    private static Rate getCurrentRate(final ExchangeDB exchangeDb, final ERTParams params) throws Exception {
        final ExchangeRate exchangeRate = exchangeDb.getLatestExchangeRate();
        if (exchangeRate != null) {
            LOGGER.info(Exchange.EXCHANGE_FILTER, "Using latest exchange rate as current exchange rate");
            return exchangeRate.getNextRate();
        }

        LOGGER.info(Exchange.EXCHANGE_FILTER, "Using default exchange rate as current exchange rate");
        return params.getDefaultRate();
    }

    /**
     * This method executes the FileContentsQuery given a Client and a FileId
     * @param client
     * @param fileId
     * @return contents of the file in byte{] format
     * @throws Exception
     */
    private static byte[] getFileContentsQuery(Client client, FileId fileId) throws Exception {
        final long getContentsQueryFee = new FileContentsQuery()
                                            .setFileId(fileId)
                                            .getCost(client);
        LOGGER.debug(Exchange.EXCHANGE_FILTER, "Cost to get file {} contents is : {}", fileId, getContentsQueryFee);
        client.setMaxQueryPayment(getContentsQueryFee);

        byte[] contentsResponse = new FileContentsQuery()
                                                        .setFileId(fileId)
                                                        .execute(client);
        return contentsResponse;
    }
}
