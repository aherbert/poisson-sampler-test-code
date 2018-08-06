package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * This test checks the WrapperAhrensDieterMarsagliaTsangGammaSampler
 */
public class AhrensDieterMarsagliaTsangGammaSamplerTest {

    /**
     * This tests the wrapper with two new samplers computes the same as the
     * original.
     */
    @Test
    public void canComputeGammaSamples() {
        final RestorableUniformRandomProvider rng = RandomSource.create(RandomSource.WELL_19937_C);
        final RandomProviderState state = rng.saveState();
        final RestorableUniformRandomProvider rng1 = RandomSource.create(RandomSource.WELL_19937_C);
        final RestorableUniformRandomProvider rng2 = RandomSource.create(RandomSource.WELL_19937_C);
        rng1.restoreState(state);
        rng2.restoreState(state);

        // Alpha is just a scaling factor so do not vary
        final double alpha = 3.6587876;

        for (int n = 0; n < 1000; n++) {
            // Small
            check(rng1, rng2, alpha, 1e-6 + rng.nextDouble());
            // Large
            check(rng1, rng2, alpha, 1 + 50 * rng.nextDouble());
        }
    }

    private static void check(final RestorableUniformRandomProvider rng1, final RestorableUniformRandomProvider rng2,
            final double alpha, final double theta) {
        AhrensDieterMarsagliaTsangGammaSampler s1 = new AhrensDieterMarsagliaTsangGammaSampler(rng1, alpha, theta);
        WrapperAhrensDieterMarsagliaTsangGammaSampler s2 = new WrapperAhrensDieterMarsagliaTsangGammaSampler(rng2,
                alpha, theta);
        for (int j = 0; j < 10; j++)
            Assert.assertEquals(s1.sample(), s2.sample(), 0);
    }
}
