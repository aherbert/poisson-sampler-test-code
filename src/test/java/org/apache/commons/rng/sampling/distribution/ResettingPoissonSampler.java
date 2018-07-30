package org.apache.commons.rng.sampling.distribution;

/**
 * This is an interface for samplers so the internal Gaussian sampler can be
 * reset. This ensures repeatability when the sampler is instantiated inside or
 * outside a loop.
 */
public interface ResettingPoissonSampler {

    /**
     * Reset the Gaussian. This just effectively clears the cached pair of Gaussian
     * numbers.
     */
    void resetGaussian();
}