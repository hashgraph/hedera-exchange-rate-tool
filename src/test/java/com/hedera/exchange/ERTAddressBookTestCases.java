package com.hedera.exchange;

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
