package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * This test checks the {@link PoissonSamplerCache} functions exactly like the
 * constructor of the {@link PoissonSampler}.
 */
public class PoissonSamplerCacheTest {
    
    // LargeMeanPoissonSampler is used at mean above 40. Set a range so that the
    // SmallMeanPoissonSampler is also required.
    
    /** The minimum of the range of the mean */
    private final int minRange = 38;
    /** The maximum of the range of the mean */
    private final int maxRange = 46;
    /** The mid-point of the range of the mean */
    private final int midRange = (minRange + maxRange) / 2;

    /**
     * Test the cache returns the same samples as the PoissonSampler when it covers
     * the entire range.
     */
    @Test
    public void canComputeSameSamplesAsPoissonSamplerWithFullRangeCache() {
        canComputeSameSamplesAsPoissonSampler(minRange, maxRange);
    }

    /**
     * Test the cache returns the same samples as the PoissonSampler with no cache.
     */
    @Test
    public void canComputeSameSamplesAsPoissonSamplerWithNoCache() {
        canComputeSameSamplesAsPoissonSampler(0, minRange - 2);
    }

    /**
     * Test the cache returns the same samples as the PoissonSampler with partial
     * cache covering the lower range.
     */
    @Test
    public void canComputeSameSamplesAsPoissonSamplerWithPartialCacheCoveringLowerRange() {
        canComputeSameSamplesAsPoissonSampler(minRange, midRange);
    }

    /**
     * Test the cache returns the same samples as the PoissonSampler with partial
     * cache covering the upper range.
     */
    @Test
    public void canComputeSameSamplesAsPoissonSamplerWithPartialCacheCoveringUpperRange() {
        canComputeSameSamplesAsPoissonSampler(midRange, maxRange);
    }

    /**
     * Test the cache returns the same samples as the PoissonSampler with cache
     * above the upper range.
     */
    @Test
    public void canComputeSameSamplesAsPoissonSamplerWithCacheAboveTheUpperRange() {
        canComputeSameSamplesAsPoissonSampler(maxRange + 10, maxRange + 20);
    }

    private void canComputeSameSamplesAsPoissonSampler(int minMean, int maxMean) {
        // Two identical RNGs
        final RestorableUniformRandomProvider rng1 = RandomSource.create(RandomSource.WELL_19937_C);
        final RandomProviderState state = rng1.saveState();
        final RestorableUniformRandomProvider rng2 = RandomSource.create(RandomSource.WELL_19937_C);
        rng2.restoreState(state);

        // Create the cache with the given range
        final PoissonSamplerCache cache = new PoissonSamplerCache(minMean, maxMean);
        for (int i = minRange; i <= maxRange; i++) {
            testPoissonSamples(rng1, rng2, cache, i);
            testPoissonSamples(rng1, rng2, cache, i + 0.5);
        }
    }

    private static void testPoissonSamples(final RestorableUniformRandomProvider rng1,
            final RestorableUniformRandomProvider rng2, PoissonSamplerCache cache, double mean) {
        final PoissonSampler s1 = new PoissonSampler(rng1, mean);
        final DiscreteSampler s2 = cache.getPoissonSampler(rng2, mean);
        for (int j = 0; j < 10; j++)
            Assert.assertEquals(s1.sample(), s2.sample());
    }
}
