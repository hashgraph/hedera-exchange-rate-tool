-- address_book was never created by a migration; existing databases have it already, so this is a no-op there.
CREATE TABLE IF NOT EXISTS address_book
(
    id             SERIAL NOT NULL,
    expirationTime BIGINT,
    addressBook    JSON,
    networkName    TEXT
);

CREATE INDEX IF NOT EXISTS address_book_expirationtime_idx ON address_book (expirationTime);
