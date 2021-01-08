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

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.exchange.database.ExchangeDB;
import com.hedera.exchange.exchanges.Exchange;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This Class represents the whole Exchange Rate Tool application. This is main entry point for the application.
 *
 * @author Anirudh, Cesar
 */
public class ExchangeRateTool {
    private static final Logger LOGGER = LogManager.getLogger(ExchangeRateTool.class);
    private static final int DEFAULT_RETRIES = 4;

    private static ERTParams ertParams;
    private static ExchangeDB exchangeDB;
    private static ERTAddressBook ertAddressBookFromPreviousRun;
    private static Set<String> updatedNetworks = new HashSet<>();

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
        while (currentTries <  maxRetries) {
            try {
                ertParams = ERTParams.readConfig(args);
                exchangeDB = ertParams.getExchangeDB();
                execute();
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

        final Map<String, Map<AccountId, String>> networks = ertParams.getNetworks();

        final long frequencyInSeconds = ertParams.getFrequencyInSeconds();
        final ExchangeRate midnightExchangeRate = exchangeDB.getLatestMidnightExchangeRate();
        final Rate midnightRate = midnightExchangeRate == null ? null : midnightExchangeRate.getNextRate();
        final Rate currentRate = getCurrentRate(exchangeDB, ertParams);
        final AccountId operatorId = AccountId.fromString(ertParams.getOperatorId());
        final ExchangeRateUtils exchangeRateUtils = new ExchangeRateUtils();

        List<Exchange> exchanges = exchangeRateUtils.generateExchanges(ertParams.getExchangeAPIList());

        final ERTproc proc = new ERTproc(ertParams.getDefaultHbarEquiv(),
                exchanges,
                ertParams.getBound(),
                ertParams.getFloor(),
                midnightRate,
                currentRate,
                frequencyInSeconds);

        final ExchangeRate exchangeRate = proc.call();

        for(String networkName : networks.keySet()) {

            if(!updatedNetworks.contains(networkName)) {
                LOGGER.info(Exchange.EXCHANGE_FILTER, "Performing File update transaction on network {}",
                        networkName);

                final Ed25519PrivateKey privateOperatorKey =
                        Ed25519PrivateKey.fromString(ertParams.getOperatorKey(networkName));
                ertAddressBookFromPreviousRun = exchangeDB.getLatestERTAddressBook(networkName);

                Map<AccountId, String> nodesForClient = ertAddressBookFromPreviousRun != null &&
                        !ertAddressBookFromPreviousRun.getNodes().isEmpty() ?
                        ertAddressBookFromPreviousRun.getNodes() :
                        networks.get(networkName);

                Client hederaClient = HederaNetworkCommunicator.buildClient(
                        nodesForClient,
                        operatorId,
                        privateOperatorKey,
                        ertParams.getMaxTransactionFee());

                if (hederaClient == null) {
                    LOGGER.error(Exchange.EXCHANGE_FILTER, "Error while building a Hedera Client");
                    throw new Exception("Couldn't Build a Hedera Client");
                }

                ERTAddressBook newAddressBook = HederaNetworkCommunicator.updateExchangeRateFile(
                        exchangeRate,
                        midnightRate,
                        hederaClient,
                        ertParams
                );

                // default 10 secs wait to close
                try {
                    hederaClient.close();
                } catch(Exception ignore) {
                    LOGGER.warn(Exchange.EXCHANGE_FILTER, "Couldn't close the hedera client properly");
                }

                exchangeDB.pushERTAddressBook(
                        exchangeRate.getNextExpirationTimeInSeconds(),
                        newAddressBook,
                        networkName
                );

                updatedNetworks.add(networkName);
            }
        }

        exchangeDB.pushExchangeRate(exchangeRate);
        if (exchangeRate.isMidnightTime()) {
            LOGGER.info(Exchange.EXCHANGE_FILTER, "This rate expires at midnight. Pushing it to the DB");
            exchangeDB.pushMidnightRate(exchangeRate);
        }
        exchangeDB.pushQueriedRate(exchangeRate.getNextExpirationTimeInSeconds(), proc.getExchangeJson());


        LOGGER.info(Exchange.EXCHANGE_FILTER, "The Exchange Rates were successfully updated");
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
}