package ru.kontur.vostok.hercules.gate.client.url.strategy;

/**
 * @author Daniil Zhenikhov
 */
public class RoundRobinUrlIterator implements UrlIterator {
    private final String[] addressPool;

    private int currentAddress = -1;
    private int currentCount = 0;
    private int retryLimit;

    public RoundRobinUrlIterator(String[] addressPool) {
        this(addressPool, 2 * addressPool.length);
    }

    public RoundRobinUrlIterator(String[] addressPool, int retryLimit) {
        this.addressPool = addressPool;
        this.retryLimit = retryLimit;
    }

    @Override
    public boolean hasNext() {
        return currentCount != retryLimit;
    }

    @Override
    public String next() {
        currentAddress = (currentAddress + 1) % addressPool.length;
        currentCount++;
        return currentCount < retryLimit ? addressPool[currentAddress] : null;
    }
}
