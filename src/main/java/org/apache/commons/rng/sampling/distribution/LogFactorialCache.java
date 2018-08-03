package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.sampling.distribution.InternalUtils.FactorialLog;

/**
 * Compute {@code log(n!)} caching values in a set range.
 * <p>
 * This is not synchronised for concurrent usage.
 */
public class LogFactorialCache {

    /** Class to compute {@code log(n!)}. This has no cached values. */
    private static final InternalUtils.FactorialLog NO_CACHE_FACTORIAL_LOG;

    static {
        // Do not cache any log(n!) values.
        // Just use this class to get the log(n!) values.
        // This makes the results exactly match the current PoissonSampler.
        NO_CACHE_FACTORIAL_LOG = FactorialLog.create();
    }

    /** The minimum N covered by the cache. */
    private final int minN;
    /** The maximum N covered by the cache. */
    private final int maxN;
    /** The cache of {@code log(n!)} value between {@link minN} and {@link maxN}. */
    private final double[] values;

    /**
     * @param minN The minimum N covered by the cache.
     * @param maxN The maximum N covered by the cache.
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    public LogFactorialCache(int minN, int maxN) {
        if (minN < 0) {
            throw new IllegalArgumentException("MinN: " + minN + " <= " + 0);
        }
        if (maxN <= minN) {
            throw new IllegalArgumentException("MaxN: " + maxN + " <= " + minN);
        }
        this.minN = minN;
        this.maxN = maxN;
        values = new double[maxN - minN + 1];
    }

    /**
     * Compute the natural logarithm of the factorial of {@code n}.
     *
     * @param n Argument.
     * @return {@code log(n!)}
     * @throws IllegalArgumentException if {@code n < 0}.
     */
    public final double factorialLog(int n) {
        if (n < minN || n > maxN)
            return NO_CACHE_FACTORIAL_LOG.value(n);
        final int index = n - minN;
        double value = values[index];
        if (value == 0)
            // Compute and store for reuse
            value = values[index] = NO_CACHE_FACTORIAL_LOG.value(n);
        return value;
    }
}
