package com.hedera.exchange.database;


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
