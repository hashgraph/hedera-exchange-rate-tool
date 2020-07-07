package com.hedera.exchange;

/*-
 * ‌
 * Hedera Exchange Rate Tool
 * ​
 * Copyright (C) 2019 - 2020 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */


import com.hedera.exchange.exchanges.Exchange;
import com.hedera.hashgraph.proto.NodeAddressBook;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.account.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.file.FileContentsQuery;
import com.hedera.hashgraph.sdk.file.FileId;
import com.hedera.hashgraph.sdk.file.FileUpdateTransaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * This Class represents the File Update logic of the Exchange Rate Tool.
 *
 * @author Anirudh, Cesar
 */
public class HederaNetworkCommunicator {

    private static final Logger LOGGER = LogManager.getLogger(HederaNetworkCommunicator.class);
    private static final String UPDATE_ERROR_MESSAGE = "The Exchange Rates were not updated successfully";
    private static final String ADDRESS_BOOK_FILE_ID = "0.0.101";

    /**
     * Method to send a File update transaction to hedera network and fetch the latest addressBook from the network.
     * @param exchangeRate
     * @param midnightRate
     * @param client
     * @param ertParams
     * @return Latest AddressBook from the Hedera Network
     * @throws HederaStatusException
     * @throws TimeoutException
     * @throws InterruptedException
     */
    public static ERTAddressBook updateExchangeRateFile(final ExchangeRate exchangeRate,
                                                        final Rate midnightRate,
                                                        Client client,
                                                        ERTParams ertParams) throws HederaStatusException, TimeoutException, InterruptedException {

        final byte[] exchangeRateAsBytes = exchangeRate.toExchangeRateSet().toByteArray();
        final AccountId operatorId = AccountId.fromString(ertParams.getOperatorId());

        final String memo = String.format("currentRate : %d, nextRate : %d, midnightRate : %d",
                exchangeRate.getCurrentRate().getCentEquiv(),
                exchangeRate.getNextRate().getCentEquiv(),
                midnightRate == null ? 0 : midnightRate.getCentEquiv());
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Memo for the FileUpdate tx : {}", memo);

        final FileId exchangeRateFileId = FileId.fromString(ertParams.getFileId());

        final Hbar currentBalance = new AccountBalanceQuery()
                .setAccountId(operatorId)
                .execute(client);

        LOGGER.info(Exchange.EXCHANGE_FILTER, "Balance before the process of updating the Exchange Rate file: {}",
                currentBalance);

        try {

            ERTAddressBook newAddressBook = fetchAddressBook(client);

            updateExchangeRateFileTxnAndValidate(exchangeRate, exchangeRateFileId, exchangeRateAsBytes, client, memo, ertParams);

            final Hbar newBalance = new AccountBalanceQuery()
                    .setAccountId(operatorId)
                    .execute(client);

            LOGGER.info(Exchange.EXCHANGE_FILTER, "Balance after updating the Exchange Rate file: {}", newBalance);

            return newAddressBook;

        } catch (Exception e) {
            e.printStackTrace();
            client.close();
        }
        return new ERTAddressBook();
    }

    /**
     * Helper Method to send the File Update and verify if the contents match after.
     * @param exchangeRate
     * @param exchangeRateFileId
     * @param exchangeRateAsBytes
     * @param client
     * @param memo
     * @param ertParams
     * @throws Exception
     */
    private static void updateExchangeRateFileTxnAndValidate(ExchangeRate exchangeRate,
                                                             FileId exchangeRateFileId,
                                                             byte[] exchangeRateAsBytes,
                                                             Client client,
                                                             String memo,
                                                             ERTParams ertParams) throws Exception {
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

    /**
     * Method to fetch the address book from the client
     * @param client  - to fetch the addressbook from
     * @return  An object of ERTAddressBook class with the
     *          contents of the address book fetched from the Client
     * @throws Exception
     */
    private static ERTAddressBook fetchAddressBook(Client client) throws Exception {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "fetching the addressbook");

        final FileId addressBookFileId = FileId.fromString(ADDRESS_BOOK_FILE_ID);
        final NodeAddressBook addressBook = NodeAddressBook.parseFrom(
                getFileContentsQuery(client, addressBookFileId));
        LOGGER.info(Exchange.EXCHANGE_FILTER, "addressbook file contents {}", addressBook);

        Map<String, String> addressBookNodes = new HashMap<>();
        if (addressBook.getNodeAddressCount() > 0) {
            addressBookNodes = ExchangeRateUtils.getNodesFromAddressBook(addressBook);
        } else {
            LOGGER.warn(Exchange.EXCHANGE_FILTER, "didnt find any addresses in the address book.");
        }

        ERTAddressBook newAddressBook = new ERTAddressBook();
        newAddressBook.setNodes(addressBookNodes);
        return  newAddressBook;
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

    /**
     * This method builds a Hedera Client
     * @param accountAddressMap
     * @param operatorId
     * @param privateKey
     * @param maxTransactoinFee
     * @return A Hedera Client or null if invalid inputs.
     */
    public static Client buildClient(Map<AccountId, String> accountAddressMap,
                                     AccountId operatorId,
                                     Ed25519PrivateKey privateKey,
                                     long maxTransactoinFee) {

        if(accountAddressMap.isEmpty() || operatorId == null || privateKey == null) {
            return null;
        }

        return new Client(accountAddressMap)
                .setMaxTransactionFee(maxTransactoinFee)
                .setOperator(operatorId, privateKey);
    }
}
