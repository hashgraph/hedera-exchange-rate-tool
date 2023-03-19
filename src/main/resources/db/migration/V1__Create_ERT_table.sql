CREATE TABLE "address_book"
(
    id             SERIAL NOT NULL,
    expirationTime BIGINT,
    exchangeRateFile JSON,
    networkname VARCHAR
);

CREATE INDEX ON address_book (expirationTime);

CREATE TABLE "exchange_rate"
(
    id             SERIAL NOT NULL,
    expirationTime BIGINT,
    exchangeRateFile JSON
);

CREATE INDEX ON exchange_rate (expirationTime);

create table midnight_rate
(
    id               SERIAL NOT NULL,
    expirationTime   BIGINT,
    exchangeRateFile JSON
);

CREATE INDEX ON midnight_rate (expirationTime);

create table queried_rate
(
    id             SERIAL NOT NULL,
    expirationTime BIGINT,
    queriedRates   JSON
);


CREATE INDEX ON queried_rate (expirationtime);
