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
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 *
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * * Neither the name of JSR-310 nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


import com.google.protobuf.ByteString;
import com.hedera.exchange.exchanges.Exchange;
import com.hedera.hashgraph.sdk.AccountBalance;
import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.FileContentsQuery;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.FileUpdateTransaction;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.proto.NodeAddressBook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.hedera.hashgraph.sdk.Client.forNetwork;

/**
 * This Class provides File Update APIs of the Exchange Rate Tool.
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
     * @param midnightExchangeRate
     * @param client
     * @param ertParams
     * @return Latest AddressBook from the Hedera Network
     * @throws TimeoutException
     * @throws InterruptedException
     */
    public ERTAddressBook updateExchangeRateFile(final ExchangeRate exchangeRate,
                                                        final ExchangeRate midnightExchangeRate,
                                                        Client client,
                                                        ERTParams ertParams) throws TimeoutException, PrecheckStatusException {

        final byte[] exchangeRateAsBytes = exchangeRate.toExchangeRateSet().toByteArray();
        final AccountId operatorId = AccountId.fromString(ertParams.getOperatorId());

        final String memo = String.format("currentRate : %.4f, nextRate : %.4f, midnight-currentRate : %.4f midnight" +
                        "-nextRate : %.4f",
                exchangeRate.getCurrentRate().getRateInUSD(),
                exchangeRate.getNextRate().getRateInUSD(),
                midnightExchangeRate == null ? 0.0 : midnightExchangeRate.getCurrentRate().getRateInUSD(),
                midnightExchangeRate == null ? 0.0 : midnightExchangeRate.getNextRate().getRateInUSD());
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Memo for the FileUpdate tx : {}", memo);

        final FileId exchangeRateFileId = FileId.fromString(ertParams.getFileId());

        final AccountBalance currentBalance = new AccountBalanceQuery()
                .setAccountId(operatorId)
                .execute(client);

        LOGGER.info(Exchange.EXCHANGE_FILTER, "Balance before the process of updating the Exchange Rate file: {}",
                currentBalance);

        try {

            ERTAddressBook newAddressBook = fetchAddressBook(client);

            updateExchangeRateFileTxn(exchangeRate, exchangeRateFileId, exchangeRateAsBytes, client, memo);
            waitForChangesToTakeEffect(ertParams.getValidationDelayInMilliseconds());
            validateUpdate(client, exchangeRateFileId, exchangeRateAsBytes);

            final AccountBalance newBalance = new AccountBalanceQuery()
                    .setAccountId(operatorId)
                    .execute(client);

            LOGGER.info(Exchange.EXCHANGE_FILTER, "Balance after updating the Exchange Rate file: {}", newBalance);

            return newAddressBook;

        } catch (Exception e) {
            LOGGER.error(Exchange.EXCHANGE_FILTER, e.getMessage());
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
     * @throws Exception
     */
    private void updateExchangeRateFileTxn(ExchangeRate exchangeRate,
                                                             FileId exchangeRateFileId,
                                                             byte[] exchangeRateAsBytes,
                                                             Client client,
                                                             String memo) throws Exception {
        LOGGER.info(Exchange.EXCHANGE_FILTER,"Pushing new ExchangeRate {}", exchangeRate.toJson());
        final TransactionResponse exchangeRateFileUpdateTransactionId = new FileUpdateTransaction()
                .setFileId(exchangeRateFileId)
                .setContents(exchangeRateAsBytes)
                .setTransactionMemo(memo)
                .execute(client);

        LOGGER.info(Exchange.EXCHANGE_FILTER,"Exchange rate file hash {} bytes and hash code {}",
                exchangeRateAsBytes.length,
                Arrays.hashCode(exchangeRateAsBytes));

        TransactionReceipt transactionReceipt = exchangeRateFileUpdateTransactionId.getReceipt(client);
        LOGGER.info(Exchange.EXCHANGE_FILTER, "First update has status {}",
                transactionReceipt.status);
    }

    /**
     * Retrieve the exchangerate file form the network and validate it with exchange rate file we just sent to make sure
     * that the file update was successful
     * @param client
     * @param exchangeRateFileId
     * @param exchangeRateAsBytes
     * @throws Exception
     */
    private void validateUpdate(Client client, FileId exchangeRateFileId, byte[] exchangeRateAsBytes) throws Exception {
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
     * we wait for some time to make sure our file update gets propagated into the network.
     * @param validationDelayInMilliseconds
     */
    private void waitForChangesToTakeEffect(long validationDelayInMilliseconds) throws InterruptedException {
        Thread.sleep(validationDelayInMilliseconds);
    }

    /**
     * Method to fetch the address book from the client
     * @param client  - to fetch the addressbook from
     * @return  An object of ERTAddressBook class with the
     *          contents of the address book fetched from the Client
     * @throws Exception
     */
    private ERTAddressBook fetchAddressBook(Client client) throws Exception {
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
    private byte[] getFileContentsQuery(Client client, FileId fileId) throws Exception {
        final Hbar getContentsQueryFee = new FileContentsQuery()
                .setFileId(fileId)
                .getCost(client);
        LOGGER.debug(Exchange.EXCHANGE_FILTER, "Cost to get file {} contents is : {}", fileId, getContentsQueryFee);
        client.setMaxQueryPayment(getContentsQueryFee);

        final ByteString contentsResponse = new FileContentsQuery()
                .setFileId(fileId)
                .execute(client);
        return contentsResponse.toByteArray();
    }

    /**
     * This method builds a Hedera Client
     * @param accountAddressMap
     * @param operatorId
     * @param privateKey
     * @param maxTransactoinFee
     * @return A Hedera Client or null if invalid inputs.
     */
    public Client buildClient(Map<String, AccountId> accountAddressMap,
                                     AccountId operatorId,
                                     PrivateKey privateKey,
                                     Hbar maxTransactoinFee) {

        if(accountAddressMap.isEmpty() || operatorId == null || privateKey == null) {
            return null;
        }

        return forNetwork(accountAddressMap)
                .setMaxTransactionFee(maxTransactoinFee)
                .setOperator(operatorId, privateKey);
    }
}
