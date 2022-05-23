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

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.exchange.database.ExchangeDB;
import com.hedera.exchange.exchanges.Exchange;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.hedera.exchange.ERTUtils.getNodesForClient;
import static com.hedera.exchange.Status.FAILED;
import static com.hedera.exchange.Status.SUCCESS;


/**
 * This Class represents the whole Exchange Rate Tool application. This is main entry point for the application.
 *
 * @author Anirudh, Cesar
 */
public class ExchangeRateTool {
    private static final Logger LOGGER = LogManager.getLogger(ExchangeRateTool.class);
    private static final Map<String, AccountId> emptyMap = Collections.emptyMap();

    static final int DEFAULT_RETRIES = 4;

    private ERTParams ertParams;
    private ExchangeDB exchangeDB;

    public static void main(final String ... args) {
        ExchangeRateTool ert = new ExchangeRateTool();
        ert.run(args);
    }

    /**
     * This method executes the ERT logic and if an execution fails, it retries for the a fixed number of times
     * mentioned in DEFAULT_RETRIES.
     * @param args
     */
    protected void run(final String ... args) {
        LOGGER.debug(Exchange.EXCHANGE_FILTER, "Starting ExchangeRateTool");
        try {
            ertParams = ERTParams.readConfig(args);
            exchangeDB = ertParams.getExchangeDB();
            execute();
        } catch (Exception ex) {
            final var subject = "FAILED : ERT Run Failed";
            final var message = ex.getMessage() + "\n";
            LOGGER.error(Exchange.EXCHANGE_FILTER, subject, ex);
            ERTNotificationHelper.publishMessage(subject, message + ExceptionUtils.getStackTrace(ex));
        }
    }

    /**
     * This method encapsulates all the execution logic
     * - Execute ERTProc
     * - generate a transaction using the operator key, file ID, sign it with the private key
     * - perform the FIle Update transaction
     * - check if the transaction was successful
     * - Write the generated exchange rate files into the Database
     * @throws IOException
     *          Throws IOException when the retrieved content from DB is not parsable.
     * @throws SQLException
     *          Throws SQL Exception when it failed to retrieve exchange rates from DB.
     * @throws TimeoutException
     *          Throws Timeout Exception when unable to complete a Hedera Transaction with in the timeout.
     */
    protected void execute() throws IOException, SQLException, TimeoutException {

        final Map<String, Map<String, AccountId>> networks = ertParams.getNetworks();

        final long frequencyInSeconds = ertParams.getFrequencyInSeconds();
        final ExchangeRate midnightExchangeRate = exchangeDB.getLatestMidnightExchangeRate();
        final Rate currentRate = getCurrentRate(exchangeDB, ertParams);
        final AccountId operatorId = AccountId.fromString(ertParams.getOperatorId());

        final List<Exchange> exchanges = ERTUtils.generateExchanges(ertParams.getExchangeAPIList());

        final ERTProcessLogic proc = new ERTProcessLogic(
                ertParams.getDefaultHbarEquiv(),
                exchanges,
                ertParams.getBound(),
                ertParams.getFloor(),
                midnightExchangeRate,
                currentRate,
                frequencyInSeconds);

        final ExchangeRate exchangeRate = proc.call();

        for(final String networkName : networks.keySet()) {
            final var status = fileUpdateTransactionForNetwork(
                    networkName,
                    operatorId,
                    exchangeRate,
                    midnightExchangeRate,
                    networks.get(networkName));
            if (status == SUCCESS) {
                exchangeDB.pushExchangeRate(exchangeRate);

                if (exchangeRate.isMidnightTime()) {
                    LOGGER.debug(Exchange.EXCHANGE_FILTER, "This rate expires at midnight. Pushing it to the DB");
                    exchangeDB.pushMidnightRate(exchangeRate);
                }

                exchangeDB.pushQueriedRate(exchangeRate.getNextExpirationTimeInSeconds(), proc.getExchangeJson());
                LOGGER.info(Exchange.EXCHANGE_FILTER, "The Exchange Rates were successfully updated");
            } else {
                final var errMessage = String.format("FAILED : The Exchange Rates were not successfully updated on %s",
                        networkName);
                ERTNotificationHelper.publishMessage(errMessage, errMessage);
                LOGGER.error(Exchange.EXCHANGE_FILTER, errMessage);
            }
        }
    }

    protected Status fileUpdateTransactionForNetwork(
            final String networkName,
            final AccountId operatorId,
            final ExchangeRate exchangeRate,
            final ExchangeRate midnightExchangeRate,
            final Map<String, AccountId> nodesFromConfig) throws IOException, SQLException, TimeoutException {
        LOGGER.info(Exchange.EXCHANGE_FILTER, "Performing File update transaction on network {}",
                networkName);

        final HederaNetworkCommunicator hnc = new HederaNetworkCommunicator(networkName);

        final PrivateKey privateOperatorKey =
                PrivateKey.fromString(ertParams.getOperatorKey(networkName));
        final ERTAddressBook ertAddressBookFromPreviousRun = exchangeDB.getLatestERTAddressBook(networkName);
        final var nodesFromPrevRun = ertAddressBookFromPreviousRun != null ?
                getNodesForClient(ertAddressBookFromPreviousRun.getNodes()) : emptyMap;

        final Map<String, AccountId> nodesForClient = nodesFromPrevRun.isEmpty() ? nodesFromPrevRun : nodesFromConfig;

        LOGGER.info(Exchange.EXCHANGE_FILTER, "Building a Hedera Client with nodes {} \n Account {}",
                nodesForClient,
                operatorId);

        try(final Client hederaClient = hnc.buildClient(
                nodesForClient,
                operatorId,
                privateOperatorKey,
                Hbar.from(ertParams.getMaxTransactionFee()))) {

            if (hederaClient == null) {
                LOGGER.error(Exchange.EXCHANGE_FILTER, "Error while building a Hedera Client");
                final var subject = String.format("ERROR : Couldn't Build a Hedera Client on %s", networkName);
                ERTNotificationHelper.publishMessage(subject, "Retrying..");
                throw new IllegalStateException(subject);
            }

            final int maxRetries = DEFAULT_RETRIES;
            int currentTries = 0;
            while (currentTries <  maxRetries) {
                try {

                    final ERTAddressBook newAddressBook = hnc.updateExchangeRateFile(
                            exchangeRate,
                            midnightExchangeRate,
                            hederaClient,
                            ertParams
                    );

                    if(!newAddressBook.getNodes().isEmpty()) {
                        exchangeDB.pushERTAddressBook(
                                exchangeRate.getNextExpirationTimeInSeconds(),
                                newAddressBook,
                                networkName
                        );
                    }
                    return SUCCESS;
                }
                catch (ReceiptStatusException rex) {
                    // Only retry if its not ReceiptStatusException as it is already handled in
                    // HederaNetworkCommunicator.updateExchangeRateFileTxn
                    LOGGER.error(Exchange.EXCHANGE_FILTER,
                            "Failed to update the network withe calculated rates with 4 retries.");
                    return FAILED;
                }
                catch (Exception ex) {
                    currentTries++;
                    LOGGER.error(Exchange.EXCHANGE_FILTER,
                            "Failed to execute at try {}/{} on network {}. Retrying. {}",
                            currentTries,
                            maxRetries,
                            networkName,
                            ex);
                }
            }
        }
        return FAILED;
    }

    /**
     * Get the current Exchange Rate from the database.
     * If not found, get the default rate from the config file.
     * @param exchangeDb
     *          Database class that we are using.
     * @param params
     *          ERTParams object to read the config file.
     * @return Rate object
     * @throws IOException
     *          Throws IOException when the retrieved content from DB is not parsable.
     * @throws SQLException
     *          Throws SQL Exception when it failed to retrieve exchange rates from DB.
     */
    private Rate getCurrentRate(final ExchangeDB exchangeDb, final ERTParams params) throws SQLException, IOException {
        final ExchangeRate exchangeRate = exchangeDb.getLatestExchangeRate();
        if (exchangeRate != null) {
            LOGGER.info(Exchange.EXCHANGE_FILTER, "Using latest exchange rate as current exchange rate");
            return exchangeRate.getNextRate();
        }

        LOGGER.info(Exchange.EXCHANGE_FILTER, "Using default exchange rate as current exchange rate");
        return params.getDefaultRate();
    }
}