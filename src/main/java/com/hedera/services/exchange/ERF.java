/**
 * This class represents the Exchange Rate File that needs to be signed and sent to network, Pricing DB
 * It holds two Rate objects that represent current and next rates
 */
package com.hedera.services.exchange;

public class ERF {
    private Rate currentRate;
    private Rate nextRate;

    public ERF(Rate currentRate, Rate nextRate){
        this.currentRate = currentRate;
        this.nextRate = nextRate;
    }

    public Rate getCurrentRate() {
        return currentRate;
    }

    public void setCurrentRate(Rate currentRate) {
        this.currentRate = currentRate;
    }

    public Rate getNextRate() {
        return nextRate;
    }

    public void setNextRate(Rate nextRate) {
        this.nextRate = nextRate;
    }
}
