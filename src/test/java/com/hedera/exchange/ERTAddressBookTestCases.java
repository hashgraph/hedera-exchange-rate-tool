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

import com.hedera.hashgraph.sdk.account.AccountId;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ERTAddressBookTestCases {

    final String addressBook = "[{\"Nodes\":{\"0.0.3\":\"34.94.254.82:50211\"," +
            "\"0.0.4\":\"35.196.34.86:50211\"," +
            "\"0.0.5\":\"35.194.75.187:50211\"," +
            "\"0.0.6\":\"34.82.241.226:50211\"}}]";

    @Test
    public void fromJson() throws IOException {

        ERTAddressBook ERTaddressBook = ERTAddressBook.fromJson(addressBook);

        assertEquals(ERTaddressBook.getNodes().get(AccountId.fromString("0.0.3")), "34.94.254.82:50211");
        assertEquals(ERTaddressBook.getNodes().get(AccountId.fromString("0.0.4")), "35.196.34.86:50211");
        assertEquals(ERTaddressBook.getNodes().get(AccountId.fromString("0.0.5")), "35.194.75.187:50211");
        assertEquals(ERTaddressBook.getNodes().get(AccountId.fromString("0.0.6")), "34.82.241.226:50211");

    }

    @Test
    public void toJson() throws IOException {
        ERTAddressBook ERTaddressBook = ERTAddressBook.fromJson(addressBook);

        assertEquals(addressBook, ERTaddressBook.toJson());
    }

}
