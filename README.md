[![codecov]()]()


# Exchange Rate Tool

This tool fetches the HBAR - USD exchange rate from all the exchanges that allow HBAR trading and calculates their weighted median [Weight is the volume of HABR - USD trading occurred on that exchange in the last 24 hours].
Once the Median is calculated we perform a smoothing operation to keep the rate in bound from the rate at previous midnight.
This Smoothed Median is then pushed to the Hedera Network[can push to multiple Hedera Networks in a single run] as the rate that is to be used for the next hour as the base for fee calculations for all transactions.

Currently 3 ERT instances run.
Instance 1 pushes rates to Mainnet
Instance 2 pushes rates to Stable Testnet
Instance 3 pushes rates to Preview Testnet, Staging-lg, Staging-sm, Integration and Performance Testing

The tool runs as a container on Kubernetes (see `Containerfile`). One image provides two processes:

* `com.hedera.exchange.server.ExchangeRateToolService` - runs the tool hourly. Must run as a single replica.
* `com.hedera.exchange.server.ExchangeRateApiServer` - serves the APIs below.

Both expose Prometheus metrics on `/metrics` and a health check on `/healthz`, on port 8080.

```
docker buildx build --platform linux/amd64 -f Containerfile -t hedera-exchange-rate-tool:local --load .
```

Configuration is read from environment variables and one config file:

* `ENDPOINT` (JDBC URL ending in `/`), `DATABASE`, `USERNAME`, `PASSWORD` - the PostgreSQL database (both processes).
* `CONFIG_PATH` - path to the config JSON (job only), e.g. a mounted ConfigMap.
* `OPERATOR_KEY_<network name>` - Hedera operator private key for every network under `Networks` in the config (job only).
* `ERT_RUN_OFFSET_SECONDS` (default 30) - seconds past the hour the job runs. The job must run as a single replica.
* `PORT` (default 8080).

Kubernetes manifests are kept outside this repository.

This tool also provides 2 APIs.

1. ExchangeRateAPI (`GET /latest`, also served as `GET /pricing` for existing clients) - This gives the latest exchange rate that this tool has pushed to the Hedera Network.
2. ExchnageRateHistoryAPI (`GET /history?no_of_records=N`, default 5) - This gives the data from the previous runs which includes
    * All the data from exchanges that it fetched.
    * The median it calculated.
    * If that median is smoothed.
    * Previous Midnight Rate.


 Exchanges that we are currently pulling latest HBAR-USD exchange rate from:
  * Coinbase
  * Binance
  * Bitmart
  * Bitstamp
  * Crypto.com
