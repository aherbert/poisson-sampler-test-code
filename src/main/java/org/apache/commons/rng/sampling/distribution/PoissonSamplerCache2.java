package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.LargeMeanPoissonSampler.LargeMeanPoissonSamplerState;

/**
 * Create a sampler for the <a href="http://mathworld.wolfram.com/PoissonDistribution.html">Poisson distribution</a>
 * using a cache to minimise construction cost.
 * <p>
 * The cache will return a sampler equivalent to
 * {@link org.apache.commons.rng.sampling.distribution#PoissonSampler(UniformRandomProvider, double)}.
 * <p>
 * The cache allows the {@link PoissonSampler} construction cost to be
 * minimised for low size Poisson samples. The cache is advantageous under 
 * the following conditions: 
 * <ul>
 * <li>The mean of the Poisson distribution falls within a known range.</li>
 * <li>The sample size to be made with the <strong>same</strong> sampler is small.</li>
 * </ul>
 * <p>
 * If the sample size to be made with the <strong>same</strong> sampler is large
 * then the construction cost is minimal compared to the sampling time.
 * <p>
 * Performance improvement is dependent on the speed of the {@link UniformRandomProvider}.
 * A fast provider can obtain a two-fold speed improvement for a single-use Poisson sampler.
 * <p>
 * The cache is <strong>not</strong> thread safe.
 */
public class PoissonSamplerCache2 {

    /**
     * The minimum N covered by the cache where {@code N = (int)Math.floor(mean)}.
     */
    private final int minN;
    /**
     * The maximum N covered by the cache where {@code N = (int)Math.floor(mean)}.
     */
    private final int maxN;
    /** The cache of states between {@link minN} and {@link maxN}. */
    private final LargeMeanPoissonSamplerState[] values;

    /**
     * @param minMean The minimum mean covered by the cache.
     * @param maxMean The maximum mean covered by the cache.
     * @throws IllegalArgumentException if {@code maxMean < minMean}
     */
    public PoissonSamplerCache2(double minMean, double maxMean) {

        // Although a mean of 0 is invalid for a Poisson sampler this case
        // is handled to make the cache user friendly. Any low means will
        // be handled by the SmallMeanPoissonSampler and not cached.
        if (minMean < 0) {
            minMean = 0;
        }
        // Allow minMean == maxMean so that the cache can be used across
        // concurrent threads to create samplers with distinct RNGs and the
        // same mean.
        if (maxMean < minMean) {
            throw new IllegalArgumentException("Max mean: " + maxMean + " < " + minMean);
        }

        // The cache can only be used for the LargeMeanPoissonSampler.
        if (maxMean < WrapperPoissonSampler.PIVOT) {
            // The upper limit is too small so no cache will be used.
            // This class will just construct new samplers.
            minN = 0;
            maxN = 0;
            values = null;
        } else {
            // Convert the mean into integers.
            // Note the minimum is clipped to the algorithm switch point.
            this.minN = (int) Math.floor(Math.max(minMean, WrapperPoissonSampler.PIVOT));
            this.maxN = (int) Math.floor(maxMean);
            values = new LargeMeanPoissonSamplerState[maxN - minN + 1];
        }
    }

    /**
     * Creates a Poisson sampler. The returned sampler will function exactly the
     * same as {@link org.apache.commons.rng.sampling.distribution#PoissonSampler(UniformRandomProvider, double)}.
     * <p>
     * A value of {@code mean} outside the range of the cache is valid. 
     * 
     * @param rng  Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @return A Poisson sampler
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    public DiscreteSampler getPoissonSampler(UniformRandomProvider rng, double mean) {

        // Ensure the same functionality as the PoissonSampler by
        // using a SmallMeanPoissonSampler under the switch point.
        if (mean < WrapperPoissonSampler.PIVOT)
            return new SmallMeanPoissonSampler(rng, mean);

        // Convert the mean into an integer.
        final int n = (int) Math.floor(mean);
        if (n > maxN)
            // Outside the range of the cache.
            return new LargeMeanPoissonSampler(rng, mean);

        // Look in the cache for a state that can be reused.
        // Note: The cache is offset by minN.
        final int index = n - minN;
        LargeMeanPoissonSamplerState state = values[index];
        if (state == null) {
            // Compute and store for reuse
            state = LargeMeanPoissonSamplerState.create(n);
            values[index] = state;
        }
        // Compute the remaining fraction of the mean
        final double lambdaFractional = mean - n;
        return new LargeMeanPoissonSampler(rng, state, lambdaFractional);
    }
}
