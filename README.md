[![CircleCI](https://circleci.com/gh/swirlds/Exchange-Rate-Tool/tree/master.svg?style=shield&circle-token=6836ac760f65328da0f419c11c155ed2c19fedae)](https://circleci.com/gh/swirlds/Exchange-Rate-Tool/tree/master)
[![codecov]()]()


# Exchange Rate Tool

This tool fetches the HBAR - USD exchange rate from all the exchanges that allow HBAR trading and calculates the median.
Once the Median is calculated we perform a smoothing operation to keep the rate in bound from the rate at previous midnight.
This Smoothed Median is then pushed to the Hedera Network as the rate that is to be used for the next hour as the base for fee calculations for all transactions.


We periodically [hourly] run this tool from AWS lambda.

This tool also provides 2 APIs.

1. ExchangeRateAPI - This gives the latest exchange rate that this tool has pushed to the Hedera Network.
2. ExchnageRateHistoryAPI - This gives the data from the previous runs which includes 
    * All the data from exchanges that it fetched.
    * The median it calculated.
    * If that median is smoothed.
    * Previous Midnight Rate.
    
    
 Exchanges that we are currently pulling latest HBAR-USD exchange rate from:
  * Liquid
  * Bittrex
  * OKCoin
  * Binance