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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.exchange.database.DBParams;
import com.hedera.exchange.database.ExchangeDB;
import com.hedera.exchange.database.ExchangeRatePostgresDB;
import com.hedera.exchange.exchanges.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class reads the parameters from the config file and provides get methods to fetch the configuration parameters.
 *
 * @author Anirudh, Cesar
 */
public class ERTParams {

    private static final Logger LOGGER = LogManager.getLogger(ERTParams.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false);

    @JsonProperty("exchanges")
    private Map<String, String> exchanges;

    @JsonProperty("exchangeRateAllowedPercentage")
    private long bound;

    @JsonProperty("Nodes")
    private Map<String, String> nodes;

    @JsonProperty("Networks")
    private Map<String, Map<String, String>> networks;

    @JsonProperty("payerAccount")
    private String payAccount;

    @JsonProperty("frequencyInSeconds")
    private long frequencyInSeconds;

    @JsonProperty("maxTransactionFee")
    private long maxTransactionFee;

    @JsonProperty("floorCentsPerHbar")
    private long floor;

    @JsonProperty("fileId")
    private String fileId;

    @JsonProperty("operatorId")
    private String operatorId;

    @JsonProperty("defaultCentEquiv")
    private int defaultCentEquiv;

    @JsonProperty("defaultHbarEquiv")
    private int defaultHbarEquiv;

    @JsonProperty("validationDelayInMilliseconds")
    private int validationDelayInMilliseconds;

    /**
     * Return a ERTParams class populated with the configuration parameters read from a local config file:
     * the path given as the first argument, or else the CONFIG_PATH environment variable.
     *
     * @param args
     * @return ERTParams object
     */
    public static ERTParams readConfig(final String[] args) {
        final String configurationPath = args != null && args.length > 0 && args[0] != null && !args[0].isBlank() ?
                args[0] : System.getenv("CONFIG_PATH");
        if (configurationPath == null || configurationPath.isBlank()) {
            throw new IllegalArgumentException(
                    "No config file given: pass its path as the first argument or set CONFIG_PATH");
        }
        return readConfig(configurationPath);
    }

    /**
     * Read the config file from a local path.
     * @param configFilePath
     *          The config file path
     * @return ERTParams object
     */
    public static ERTParams readConfig(final String configFilePath) {

        LOGGER.debug(Exchange.EXCHANGE_FILTER, "Reading config from {}", configFilePath);

        try (final FileReader configFile = new FileReader(configFilePath)){
            final ERTParams ertParams = OBJECT_MAPPER.readValue(configFile, ERTParams.class);
            LOGGER.info(Exchange.EXCHANGE_FILTER, "Config file is read successfully");
            return ertParams;
        } catch (final FileNotFoundException ex) {
            final String exceptionMessage = String.format("Reading config from %s failed. File is not found.", configFilePath);
            LOGGER.error(Exchange.EXCHANGE_FILTER, exceptionMessage);
            throw new IllegalArgumentException(exceptionMessage, ex);
        } catch (final IOException ex) {
            final String exceptionMessage = String.format("Failed to load configuration %s", configFilePath);
            LOGGER.error(Exchange.EXCHANGE_FILTER, exceptionMessage);
            throw new RuntimeException(exceptionMessage);
        }
    }

    /**
     * Converts the ERTParams object into a Json string using OBJECT_MAPPER
     * @return Json String
     * @throws JsonProcessingException
     *      Throw when failed to parse json as string.
     */
    public String toJson() throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(this);
    }

    /**
     * Returns a Map of Exchanges and their APIs to get the HBAR rates.
     * @return Map<String, String> of ExchangeName:ExchangeURL
     */
    public Map<String, String> getExchangeAPIList() {
        return exchanges;
    }

    /**
     * Return the bound
     * @return bound
     */
    public long getBound() {
        return bound;
    }

    /**
     * Get the default HbarEquiv value
     * @return
     */
    public int getDefaultHbarEquiv() {
        return this.defaultHbarEquiv;
    }

    /**
     * Get the frequency at which ERT should be running.
     * @return
     */
    public long getFrequencyInSeconds() {
        return this.frequencyInSeconds;
    }

    /**
     * Return the networks ERT is sending the ERT file update to.
     * @return Map of Network name and its Node's AccountID to its IpAddress.
     */
    public Map<String, Map<String, AccountId>> getNetworks() {
        final Map<String, Map<String, AccountId>> networkAddresses = new HashMap<>();
        for(final Map.Entry<String, Map<String, String>> network : this.networks.entrySet()) {
            Map<String, AccountId> accountToNodeAddresses = new HashMap<>();
            for( final Map.Entry<String, String> node : network.getValue().entrySet()) {
                final AccountId nodeId = AccountId.fromString(node.getKey());
                accountToNodeAddresses.put(node.getValue(), nodeId);
            }
            networkAddresses.put(network.getKey(), accountToNodeAddresses);
        }
        return networkAddresses;
    }

    /**
     * Get the Pay account to execute this ER file update transaction.
     * @return account ID in string
     */
    public String getPayAccount() {
        return payAccount;
    }

    /**
     * Get the max transaction fee for file update.
     * @return
     */
    public long getMaxTransactionFee() {
        return this.maxTransactionFee;
    }

    /**
     * Get Exchange Rate file ID
     * @return
     */
    public String getFileId() {
        return this.fileId;
    }

    /**
     * Get the operator ID - Account from which the FIle update transaction is performed
     * @return
     */
    public String getOperatorId() {
        return this.operatorId;
    }

    /**
     * Get the Floor of the Exchange Rate that is allowed.
     * @return
     */
    public long getFloor(){ return this.floor; }

    @JsonIgnore
    public String getOperatorKey(String networkName) {
        return System.getenv("OPERATOR_KEY_" + networkName);
    }

    /**
     * Get the default Exchange Rate
     * @return
     */
    public Rate getDefaultRate() {
        return new Rate(this.defaultHbarEquiv, this.defaultCentEquiv, ERTUtils.getCurrentExpirationTime());
    }

    /**
     * Get the validation Delay in Milli Seconds
     * @return
     */
    public long getValidationDelayInMilliseconds() {
        return this.validationDelayInMilliseconds;
    }

    /**
     * Get the Database class to read and write the Exchange Rate Files.
     * @return ExchangeRateDb object backed by PostgreSQL.
     */
    public ExchangeDB getExchangeDB() {
        return new ExchangeRatePostgresDB(new DBParams());
    }
}
