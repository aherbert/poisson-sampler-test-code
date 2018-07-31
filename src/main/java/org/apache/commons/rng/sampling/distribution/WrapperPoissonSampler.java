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
 *  <li>
 *   For large means, we use the rejection algorithm described in
 *   <blockquote>
 *    Devroye, Luc. (1981).<i>The Computer Generation of Poisson Random Variables</i><br>
 *    <strong>Computing</strong> vol. 26 pp. 197-207.
 *   </blockquote>
 *  </li>
 * </ul>
 * 
 * This class wraps the {@link SmallMeanPoissonSampler} and {@link LargeMeanPoissonSampler},
 * choosing the appropriate sampler based on the {@code mean}.
 */
public class WrapperPoissonSampler 
    implements DiscreteSampler {

    /** Value for switching sampling algorithm. */
    static final double PIVOT = 40;
    /** The internal Poisson sampler. */
    private final DiscreteSampler poissonSampler;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    public WrapperPoissonSampler(UniformRandomProvider rng, double mean) {
        // Delegate all work to specialised samplers. 
        // These should check the input arguments.
        poissonSampler = mean < PIVOT 
                ? new SmallMeanPoissonSampler(rng, mean) 
                : new LargeMeanPoissonSampler(rng, mean);
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        return poissonSampler.sample();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Poisson deviate [" + super.toString() + "]";
    }
}