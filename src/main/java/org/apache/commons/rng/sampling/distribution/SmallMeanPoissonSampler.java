package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Sampler for the <a href="http://mathworld.wolfram.com/PoissonDistribution.html">Poisson distribution</a>.
 *
 * <ul>
 *  <li>
 *   For small means, a Poisson process is simulated using uniform deviates, as
 *   described <a href="http://mathaa.epfl.ch/cours/PMMI2001/interactive/rng7.htm">here</a>.
 *   The Poisson process (and hence, the returned value) is bounded by 1000 * mean.
 *  </li>
 * </ul>
 * 
 * This sampler is suitable for {@code mean<40}.
 */
public class SmallMeanPoissonSampler
    extends SamplerBase
    implements DiscreteSampler {

    /** Mean of the distribution. */
    final double mean;
    /** 
     * Pre-compute {@code Math.exp(-mean)}. 
     * Note: This is the probability of the Poisson sample {@code P(n=0)}.
     */
    final double p0;
    /** Pre-compute {@code 1000 * mean} as the upper limit of the sample. */
    final int limit;

    /**
     * @param rng  Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    SmallMeanPoissonSampler(UniformRandomProvider rng,
                            double mean) {
        super(rng);
        if (mean <= 0) {
            throw new IllegalArgumentException(mean + " <= " + 0);
        }
        
        this.mean = mean;
        p0 = Math.exp(-mean);
        // The returned sample is bounded by 1000 * mean or Integer.MAX_VALUE
        limit = (int) Math.ceil(Math.min(1000 * mean, Integer.MAX_VALUE));
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        int n = 0;
        double r = 1;

        while (n < limit) {
            r *= nextDouble();
            if (r >= p0) {
                n++;
            } else {
                break;
            }
        }
        return n;
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Small Mean Poisson deviate [" + super.toString() + "]";
    }
}
