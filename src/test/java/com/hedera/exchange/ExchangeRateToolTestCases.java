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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.hedera.hashgraph.proto.NodeAddressBook;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExchangeRateToolTestCases {
    private Map<String, String> nodes =  new HashMap<>();
    private NodeAddressBook addressBook;

    @Test
    public void verifyNodesFromAddressBook() throws IOException {
        setup();

        Map<String, String> ERTnodes = ExchangeRateUtils.getNodesFromAddressBook(addressBook);
        for( String node : ERTnodes.keySet()){
            assertEquals(ERTnodes.get(node), nodes.get(node));
        }
    }

    public void setup() throws IOException {
        File addressBookFile = new File("src/test/resources/addressBook.bin");
        FileInputStream fis = new FileInputStream(addressBookFile);
        byte[] content = new byte[(int) addressBookFile.length()];
        fis.read(content);
        addressBook = NodeAddressBook.parseFrom(content);

        nodes.put("0.0.3", "35.237.182.66:50211");
        nodes.put("0.0.4", "35.245.226.22:50211");
        nodes.put("0.0.5", "34.68.9.203:50211");
        nodes.put("0.0.6", "34.83.131.197:50211");
        nodes.put("0.0.7", "34.94.236.63:50211");
        nodes.put("0.0.8", "35.203.26.115:50211");
        nodes.put("0.0.9", "34.77.3.213:50211");
        nodes.put("0.0.10", "35.197.237.44:50211");
        nodes.put("0.0.11", "35.246.250.176:50211");
        nodes.put("0.0.12", "34.90.117.105:50211");
        nodes.put("0.0.13", "35.200.57.21:50211");
        nodes.put("0.0.14", "34.92.120.143:50211");
        nodes.put("0.0.15", "34.87.47.168:50211");
    }

}
