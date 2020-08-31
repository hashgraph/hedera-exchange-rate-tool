package com.hedera.exchange.database;

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


import com.google.cloud.spanner.*;
import com.hedera.exchange.ExchangeRate;

public class GCPExchangeRateDB {

    // copy instance and db id from spanner website
    private static final String projectId = "ert-public-test-1";
    private static final String instanceId = "ert-spanner-instance";
    private static final String databaseId = "ert_data";

    public static void pushExchangeRate(ExchangeRate exchangeRate){
        // Instantiates a client
        SpannerOptions options = SpannerOptions.newBuilder().setProjectId(projectId).build();
        Spanner spanner = options.getService();
        try{
            // Creates a database client
            DatabaseClient dbClient = spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));

            String sql = String.format("INSERT INTO ExchangeRate (ExpirationTime, ExchagneRateData) values ('{}', {})",
                    exchangeRate.getNextExpirationTimeInSeconds(), exchangeRate.toString());

            dbClient.readWriteTransaction().run(
                    new TransactionRunner.TransactionCallable<Void>() {
                        @Override
                        public Void run(TransactionContext transaction) throws Exception {
                            long rowCount = transaction.executeUpdate(Statement.of(sql));
                            System.out.printf("%d record inserted.\n", rowCount);
                            return null;
                        }
                    }
            );

        }
        catch( Exception e ){
            System.out.println("Writing to DB failed : " + e.getMessage());
        }
        finally {
            spanner.close();
        }
    }

    public static void pushMidnightExchangeRate(ExchangeRate exchangeRate){
        // Instantiates a client
        SpannerOptions options = SpannerOptions.newBuilder().setProjectId(projectId).build();
        Spanner spanner = options.getService();
        try{
            // Creates a database client
            DatabaseClient dbClient = spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));

            String sql = String.format("INSERT INTO MidnightExchangeRate (ExpirationTime, ExchangeRateData) values ('{}', {})",
                    exchangeRate.getNextExpirationTimeInSeconds(), exchangeRate.toJson());

            dbClient.readWriteTransaction().run(
                    new TransactionRunner.TransactionCallable<Void>() {
                        @Override
                        public Void run(TransactionContext transaction) throws Exception {
                            long rowCount = transaction.executeUpdate(Statement.of(sql));
                            System.out.printf("%d record inserted.\n", rowCount);
                            return null;
                        }
                    }
            );

        }
        catch( Exception e ){
            System.out.println("Writing to DB failed : " + e.getMessage());
        }
        finally {
            spanner.close();
        }
    }

    // TODO build a DS to hold all the queries to exchanges and data they sent back
    public static void pushQueriedExchangesData(){
        // Instantiates a client
        SpannerOptions options = SpannerOptions.newBuilder().setProjectId(projectId).build();
        Spanner spanner = options.getService();
        try{
            // Creates a database client
            DatabaseClient dbClient = spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));

            String sql = String.format("INSERT INTO ExcahngesData (ExpirationTime, ExchangesData) values ('{}', {})",
                   "" , "");

            dbClient.readWriteTransaction().run(
                    new TransactionRunner.TransactionCallable<Void>() {
                        @Override
                        public Void run(TransactionContext transaction) throws Exception {
                            long rowCount = transaction.executeUpdate(Statement.of(sql));
                            System.out.printf("%d record inserted.\n", rowCount);
                            return null;
                        }
                    }
            );

        }
        catch( Exception e ){
            System.out.println("Writing to DB failed : " + e.getMessage());
        }
        finally {
            spanner.close();
        }
    }

    public static void getMidnightExchange(long expirationTime){
        // Instantiates a client
        SpannerOptions options = SpannerOptions.newBuilder().setProjectId(projectId).build();
        Spanner spanner = options.getService();
        try{
            // Creates a database client
            DatabaseClient dbClient = spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));

            String sql = String.format("SELECT ExchangeRateData FROM MidnightExchangeRate where ExpirationTime = {}",
                    expirationTime);

            ResultSet resultSet = dbClient.singleUse().executeQuery(Statement.of(sql));

            // TODO map the result set to exchange rate.

        }
        catch( Exception e ){
            System.out.println("Writing to DB failed : " + e.getMessage());
        }
        finally {
            spanner.close();
        }
    }


    public static void main(){
        // copy instance and db id from spanner website
        String projectId = "ert-public-test-1";
        String instanceId = "ert-spanner-instance";
        String databaseId = "ert_data";
        // Instantiates a client
        SpannerOptions options = SpannerOptions.newBuilder().setProjectId(projectId).build();
        Spanner spanner = options.getService();


        try{
            // Creates a database client
            DatabaseClient dbClient = spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));

            dbClient.readWriteTransaction().run(
                    new TransactionRunner.TransactionCallable<Void>() {
                        @Override
                        public Void run(TransactionContext transaction) throws Exception {
                            String sql = "INSERT INTO ExchangeRate (ExpirationTime, ExchagneRateData) " +
                                    "values ('1567022400', '1,15,1567018800;1,14,1567022400')";
                            long rowCount = transaction.executeUpdate(Statement.of(sql));
                            System.out.printf("%d record inserted.\n", rowCount);
                            return null;
                        }
                    }
            );

        }
        catch( Exception e ){
            System.out.println("Writing to DB failed : " + e.getMessage());
        }
        finally {
            spanner.close();
        }
    }
}
