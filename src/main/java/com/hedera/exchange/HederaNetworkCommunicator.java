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
import com.google.protobuf.InvalidProtocolBufferException;
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
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.proto.NodeAddressBook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.hedera.exchange.ExchangeRateTool.DEFAULT_RETRIES;
import static com.hedera.hashgraph.sdk.Client.forNetwork;
import static com.hedera.hashgraph.sdk.Status.EXCHANGE_RATE_CHANGE_LIMIT_EXCEEDED;
import static com.hedera.hashgraph.sdk.Status.SUCCESS;

/**
 * This Class provides File Update APIs of the Exchange Rate Tool.
 *
 * @author Anirudh, Cesar
 */
public class HederaNetworkCommunicator {

    private static final Logger LOGGER = LogManager.getLogger(HederaNetworkCommunicator.class);
    private static final String UPDATE_ERROR_MESSAGE =
            "The Exchange Rate file contents defer from what we pushed.";
    private static final String ADDRESS_BOOK_FILE_ID = "0.0.101";

    private final String networkName;

    public HederaNetworkCommunicator(final String networkName) {
        this.networkName = networkName;
    }

    /**
     * Method to send a File update transaction to hedera network and fetch the latest addressBook from the network.
     * @param exchangeRate
     *          The Exchange rate File to send to the network.
     * @param midnightExchangeRate
     *          The midnight exchange rate for the network.
     * @param client
     *           hedera client for sending file update transaction.
     * @param ertParams
     * @return Latest AddressBook from the Hedera Network.
     * @throws TimeoutException
     *          Timeout exception for the file update transaction.
     * @throws PrecheckStatusException
     *          precheck failed exception file update transaction.
     * @throws IOException
     *          Json conversion exception.
     * @throws ReceiptStatusException
     *          Exception when the network rejects the Exchange rate file update transaction.
     * @throws InterruptedException
     *          Exception thrown when waiting for effects of File Update transaction.
     */
    public ERTAddressBook updateExchangeRateFile(
            final ExchangeRate exchangeRate,
            final ExchangeRate midnightExchangeRate,
            final Client client,
            final ERTParams ertParams)
            throws TimeoutException, PrecheckStatusException, IOException, ReceiptStatusException, InterruptedException {
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

        LOGGER.info(Exchange.EXCHANGE_FILTER, "Balance before updating the Exchange Rate file: {}",
                currentBalance.hbars.toString());

//        ERTAddressBook newAddressBook = new ERTAddressBook();
        ERTAddressBook newAddressBook = fetchAddressBook(client);

        updateExchangeRateFileTxn(exchangeRate, exchangeRateFileId, exchangeRateAsBytes, client, memo, ertParams.getRegion());

        waitForChangesToTakeEffect(ertParams.getValidationDelayInMilliseconds());

        validateUpdate(client, exchangeRateFileId, exchangeRateAsBytes);

        final AccountBalance newBalance = new AccountBalanceQuery()
                .setAccountId(operatorId)
                .execute(client);

        LOGGER.info(Exchange.EXCHANGE_FILTER, "Balance after updating the Exchange Rate file: {}",
                newBalance.hbars.toString());

        return newAddressBook;
    }

    /**
     * Helper Method to send the File Update and verify if the contents match after.
     * @param exchangeRate
     *          The Exchange rate File to send to the network
     * @param exchangeRateFileId
     *          The ExchangeRateFile Id for the network
     * @param exchangeRateAsBytes
     *          The contents of Exchange Rate file to upload as a byte[]
     * @param client
     *          hedera client for sending file update transaction
     * @param memo
     *          Memo for the file update transaction
     * @throws TimeoutException
     *          Timeout exception for the file update transaction
     * @throws PrecheckStatusException
     *          precheck failed exception file update transaction
     * @throws IOException
     *          Json conversion exception
     * @throws ReceiptStatusException
     *          Exception when the network rejects the Exchange rate file update transaction.
     */
    private void updateExchangeRateFileTxn(
            ExchangeRate exchangeRate,
            final FileId exchangeRateFileId,
            final byte[] exchangeRateAsBytes,
            final Client client,
            final String memo,
            final String region)
            throws TimeoutException, PrecheckStatusException, IOException, ReceiptStatusException {
        int retryCount = 1;
        TransactionReceipt transactionReceipt;
        while(true) {
            try {
                LOGGER.info(Exchange.EXCHANGE_FILTER, "Pushing new ExchangeRate {}", exchangeRate.toJson());

                final TransactionResponse response = new FileUpdateTransaction()
                        .setFileId(exchangeRateFileId)
                        .setContents(exchangeRate.toExchangeRateSet().toByteArray())
                        .setTransactionMemo(memo)
                        .execute(client);

                LOGGER.debug(Exchange.EXCHANGE_FILTER, "Exchange rate file hash {} bytes and hash code {}",
                        exchangeRateAsBytes.length,
                        Arrays.hashCode(exchangeRateAsBytes));

                transactionReceipt = response.getReceipt(client);
                if(transactionReceipt.status.toString().equals(SUCCESS.toString())) {
                    LOGGER.info(Exchange.EXCHANGE_FILTER, "File update has status {}",
                            SUCCESS.toString());
                    return;
                }
            } catch (ReceiptStatusException ex) {
                var status = ex.receipt.status;
                if(status == EXCHANGE_RATE_CHANGE_LIMIT_EXCEEDED) {
                    String subject = String.format("%s : ReceiptStatusException : %s", networkName, status);
                    String retryMessage = String.format("Run %d/%d Failed. Retrying with a new rate that is closer " +
                            "to rate in receipt.", retryCount, DEFAULT_RETRIES);
                    String proposedRate = exchangeRate.toJson();
                    LOGGER.error(Exchange.EXCHANGE_FILTER, subject);
                    LOGGER.debug(Exchange.EXCHANGE_FILTER, retryMessage);

                    transactionReceipt = ex.receipt;

                    LOGGER.debug(Exchange.EXCHANGE_FILTER, "{} update has status {}", retryCount,
                            transactionReceipt.status);

                    com.hedera.hashgraph.sdk.ExchangeRate activeRateFromReceipt = transactionReceipt.exchangeRate;

                    String rateInNetwork = activeRateFromReceipt.toString();

                    LOGGER.info(Exchange.EXCHANGE_FILTER, "Exchange Rates from receipt {}", rateInNetwork);
                    retryMessage = String.format("ERROR : %s \n proposed rate : %s \n Rates on Network %s",
                            retryMessage, proposedRate, rateInNetwork);
                    ERTNotificationHelper.publishMessage(subject, retryMessage, region);

                    Rate activeRate = new Rate(activeRateFromReceipt.hbars,
                            activeRateFromReceipt.cents,
                            activeRateFromReceipt.expirationTime.getEpochSecond());


                    exchangeRate = ERTUtils.calculateNewExchangeRate(activeRate, exchangeRate);

                    if (retryCount++ == DEFAULT_RETRIES) {
                        throw ex;
                    }
                } else {
                    throw ex;
                }
            } catch (PrecheckStatusException ex) {
                var subject = String.format("ERROR : %s : PreCheckStatusException : %s", networkName, ex.status);
                LOGGER.error(Exchange.EXCHANGE_FILTER, subject, ex);
                ERTNotificationHelper.publishMessage(subject, ex.getMessage(), region);
                if( retryCount++ == DEFAULT_RETRIES ) {
                    throw ex;
                }
            }
        }
    }

    /**
     * Retrieve the exchangeRate file form the network and validate it with exchange rate file we just sent to make sure
     * that the file update was successful
     * @param client
     *          A hedera client that is used to submit the transaction.
     * @param exchangeRateFileId
     *          File Id of the Exchange rate file on the network
     * @param exchangeRateAsBytes
     *          The exchange rate file data in byte array for the transaction.
     * @throws TimeoutException
     *          Query timeout exception
     * @throws PrecheckStatusException
     *          PreCheck failed for the query exception
     */
    private void validateUpdate(
            final Client client,
            final FileId exchangeRateFileId,
            final byte[] exchangeRateAsBytes)
            throws TimeoutException, PrecheckStatusException {
        final byte[] contentsRetrieved =  getFileContentsQuery(client, exchangeRateFileId);

        LOGGER.debug("The contents retrieved has {} bytes and hash code {}",
                contentsRetrieved.length,
                Arrays.hashCode(contentsRetrieved));
        if (!Arrays.equals(exchangeRateAsBytes, contentsRetrieved)) {
            LOGGER.error(Exchange.EXCHANGE_FILTER, UPDATE_ERROR_MESSAGE);
            throw new IllegalStateException(UPDATE_ERROR_MESSAGE);
        }
    }

    /**
     * we wait for some time to make sure our file update gets propagated into the network.
     * @param validationDelayInMilliseconds
     *          Time to wait for after the file update before retrieving the contents in milli seconds.
     */
    private void waitForChangesToTakeEffect(final long validationDelayInMilliseconds) throws InterruptedException {
        Thread.sleep(validationDelayInMilliseconds);
    }

    /**
     * Method to fetch the address book from the client
     * @param client  - to fetch the addressbook from
     * @return  An object of ERTAddressBook class with the
     *          contents of the address book fetched from the Client
     * @throws TimeoutException
     *          Timeout exception for AddressBook download transaction
     * @throws PrecheckStatusException
     *          precheck failed exception for AddressBook download transaction
     * @throws InvalidProtocolBufferException
     *          Invalid Downloaded contents exception
     */
    private ERTAddressBook fetchAddressBook(final Client client) throws TimeoutException, PrecheckStatusException, InvalidProtocolBufferException {
        LOGGER.debug(Exchange.EXCHANGE_FILTER, "fetching the addressBook");

        final FileId addressBookFileId = FileId.fromString(ADDRESS_BOOK_FILE_ID);
        final NodeAddressBook addressBook = NodeAddressBook.parseFrom(
                getFileContentsQuery(client, addressBookFileId));
        LOGGER.debug(Exchange.EXCHANGE_FILTER, "addressBook file contents {}", addressBook);

        Map<String, String> addressBookNodes = new HashMap<>();
        if (addressBook.getNodeAddressCount() > 0) {
            addressBookNodes = ERTUtils.getNodesFromAddressBook(addressBook);
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
     *          Hedera client to submit the FileContentsQuery
     * @param fileId
     *          Id of the file to download from the network
     * @return contents of the file in byte{] format
     * @throws TimeoutException
     *          Timeout exception for the FileContentsQuery
     * @throws PrecheckStatusException
     *          PreCheck failed exception for the FileContentsQuery
     */
    private byte[] getFileContentsQuery(final Client client, final FileId fileId) throws TimeoutException, PrecheckStatusException {
        final Hbar getContentsQueryFee = new FileContentsQuery()
                .setFileId(fileId)
                .getCost(client);
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Cost to get file {} contents is : {}", fileId, getContentsQueryFee);
        client.setDefaultMaxQueryPayment(Hbar.from(1L));

        final ByteString contentsResponse = new FileContentsQuery()
                .setFileId(fileId)
                .execute(client);
        return contentsResponse.toByteArray();
    }

    /**
     * This method builds a Hedera Client.
     * @param accountAddressMap
     *          The AddressBook of the network.
     * @param operatorId
     *          The payer Id for the transaction.
     * @param privateKey
     *          The key of the payer.
     * @param maxTransactionFee
     *          The maximum transaction fee for the transactions.
     * @return A Hedera Client or null if invalid inputs.
     */
    public Client buildClient(
            final Map<String, AccountId> accountAddressMap,
            final AccountId operatorId,
            final PrivateKey privateKey,
            final Hbar maxTransactionFee) {
        if(accountAddressMap.isEmpty() || operatorId == null || privateKey == null) {
            return null;
        }

        return forNetwork(accountAddressMap)
                .setMaxTransactionFee(maxTransactionFee)
                .setOperator(operatorId, privateKey);
    }
}
