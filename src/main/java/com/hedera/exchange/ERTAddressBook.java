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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedera.hashgraph.sdk.account.AccountId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class saves the address book retrieved from the nodes and converts to a json format to save it
 * to the database.
 */
public class ERTAddressBook {
    private static final Logger LOGGER = LogManager.getLogger(ERTAddressBook.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @JsonProperty("Nodes")
    private Map<String, String> nodes;

    public ERTAddressBook(){
        nodes = new HashMap<>();
    }

    /**
     * Converts a Json string into an ERTAddressBook object
     * @param json String that represents a ERTAddressBook.
     * @return ERTAddressBook object
     * @throws IOException
     */
    public static ERTAddressBook fromJson(final String json) throws IOException {
        try {
            final ERTAddressBook[] ertAddressBooks = OBJECT_MAPPER.readValue(json, ERTAddressBook[].class);
            return ertAddressBooks[0];
        } catch (final Exception ex) {
            return OBJECT_MAPPER.readValue(json, ERTAddressBook.class);
        }
    }

    /**
     * Converts the ERTAddressBook object into a Json String
     * @return Json String
     * @throws JsonProcessingException
     */
    public String toJson() throws JsonProcessingException {
        final ERTAddressBook[] ertAddressBooks = new ERTAddressBook[] { this };
        return OBJECT_MAPPER.writeValueAsString(ertAddressBooks);
    }

    public void setNodes(Map<String, String> nodes) {
        this.nodes = nodes;
    }

    /**
     * Converts the string - string mapping of node id and address in the addressbook to
     * AccountID - string map so that, it can be used in hedera client directly.
     * @return
     */
    public Map<AccountId, String> getNodes() {
        final Map<AccountId, String> accountToNodeAddresses = new HashMap<>();
        for (final Map.Entry<String, String> node : this.nodes.entrySet()) {
            final AccountId nodeId = AccountId.fromString(node.getKey());
            accountToNodeAddresses.put(nodeId, node.getValue());
        }
        return accountToNodeAddresses;
    }
}
