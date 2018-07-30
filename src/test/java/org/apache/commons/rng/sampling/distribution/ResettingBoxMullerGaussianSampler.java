package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * This is a copy of the {@link ResettingBoxMullerGaussianSampler} modified so the sampler
 * can be reset to ensure all tests generate the same random samples.
 */
public class ResettingBoxMullerGaussianSampler
extends SamplerBase
implements ContinuousSampler {
    /** Next gaussian. */
    private double nextGaussian = Double.NaN;
    /** Mean. */
    private final double mean;
    /** standardDeviation. */
    private final double standardDeviation;
    
    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param mean Mean of the Gaussian distribution.
     * @param standardDeviation Standard deviation of the Gaussian distribution.
     */
    public ResettingBoxMullerGaussianSampler(UniformRandomProvider rng,
                                    double mean,
                                    double standardDeviation) {
        super(rng);
        this.mean = mean;
        this.standardDeviation = standardDeviation;
    }
    
    /** {@inheritDoc} */
    public double sample() {
        final double random;
        if (Double.isNaN(nextGaussian)) {
            // Generate a pair of Gaussian numbers.
    
            final double x = nextDouble();
            final double y = nextDouble();
            final double alpha = 2 * Math.PI * x;
            final double r = Math.sqrt(-2 * Math.log(y));
    
            // Return the first element of the generated pair.
            random = r * Math.cos(alpha);
    
            // Keep second element of the pair for next invocation.
            nextGaussian = r * Math.sin(alpha);
        } else {
            // Use the second element of the pair (generated at the
            // previous invocation).
            random = nextGaussian;
    
            // Both elements of the pair have been used.
            nextGaussian = Double.NaN;
        }
    
        return standardDeviation * random + mean;
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Box-Muller Gaussian deviate [" + super.toString() + "]";
    }
    
    /**
	 * Reset the sampler.
	 */
    public void reset() {
        nextGaussian = Double.NaN;
    }
}
