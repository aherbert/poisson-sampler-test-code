package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.InternalUtils.FactorialLog;

/**
 * Sampler for the
 * <a href="http://mathworld.wolfram.com/PoissonDistribution.html">Poisson
 * distribution</a>.
 *
 * <ul>
 * <li>For large means, we use the rejection algorithm described in <blockquote>
 * Devroye, Luc. (1981).<i>The Computer Generation of Poisson Random
 * Variables</i><br>
 * <strong>Computing</strong> vol. 26 pp. 197-207. </blockquote></li>
 * </ul>
 * 
 * This sampler is suitable for {@code mean>=40}.
 */
public class LargeMeanPoissonSampler extends SamplerBase implements DiscreteSampler {

    /** Class to compute {@code log(n!)}. This has no cached values. */
    private static final InternalUtils.FactorialLog NO_CACHE_FACTORIAL_LOG;

    static {
        // Do not cache any log(n!) values.
        // Just use this class to get the log(n!) values.
        // This makes the results exactly match the current PoissonSampler.
        NO_CACHE_FACTORIAL_LOG = FactorialLog.create();
    }

    /** Exponential. */
    private final ContinuousSampler exponential;
    /** Gaussian. */
    private final ContinuousSampler gaussian;
    /** Local class to compute {@code log(n!)}. This may have cached values. */
    private final InternalUtils.FactorialLog factorialLog;

    // Working values
    private final double lambda;
    private final double lambdaFractional;
    private final double logLambda;
    private final double logLambdaFactorial;
    private final double delta;
    private final double halfDelta;
    private final double twolpd;
    private final double p1;
    private final double p2;
    private final double c1;

    /** The internal Poisson sampler for the lambda fraction. */
    private final DiscreteSampler smallMeanPoissonSampler;

    /**
     * Encapsulate the state of the sampler. The state is valid for construction of
     * a sampler in the range {@code lambda <= mean < lambda+1}.
     */
    static class LargeMeanPoissonSamplerState {
        private final double lambda;
        private final double logLambda;
        private final double logLambdaFactorial;
        private final double delta;
        private final double halfDelta;
        private final double twolpd;
        private final double p1;
        private final double p2;
        private final double c1;

        private LargeMeanPoissonSamplerState(double lambda, double logLambda, double logLambdaFactorial, double delta,
                double halfDelta, double twolpd, double p1, double p2, double c1) {
            this.lambda = lambda;
            this.logLambda = logLambda;
            this.logLambdaFactorial = logLambdaFactorial;
            this.delta = delta;
            this.halfDelta = halfDelta;
            this.twolpd = twolpd;
            this.p1 = p1;
            this.p2 = p2;
            this.c1 = c1;
        }

        /**
         * Creates the state. The state is valid for construction of a sampler in the
         * range {@code n <= mean < n+1}.
         *
         * @param n the value n ({@code floor(mean)})
         * @return the state
         * @throws IllegalArgumentException if {@code n < 0}.
         */
        static LargeMeanPoissonSamplerState create(int n) {
            if (n < 0) {
                throw new IllegalArgumentException(n + " < " + 0);
            }
            final double lambda = n;
            final double logLambda = Math.log(lambda);
            final double logLambdaFactorial = NO_CACHE_FACTORIAL_LOG.value(n);
            final double delta = Math.sqrt(lambda * Math.log(32 * lambda / Math.PI + 1));
            final double halfDelta = delta / 2;
            final double twolpd = 2 * lambda + delta;
            final double c1 = 1 / (8 * lambda);
            final double a1 = Math.sqrt(Math.PI * twolpd) * Math.exp(c1);
            final double a2 = (twolpd / delta) * Math.exp(-delta * (1 + delta) / twolpd);
            final double aSum = a1 + a2 + 1;
            final double p1 = a1 / aSum;
            final double p2 = a2 / aSum;
            return new LargeMeanPoissonSamplerState(lambda, logLambda, logLambdaFactorial, delta, halfDelta, twolpd, p1,
                    p2, c1);
        }
    }

    /**
     * @param rng  Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    LargeMeanPoissonSampler(UniformRandomProvider rng, double mean) {
        super(rng);
        if (mean <= 0) {
            throw new IllegalArgumentException(mean + " <= " + 0);
        }

        gaussian = new BoxMullerGaussianSampler(rng, 0, 1);
        exponential = new AhrensDieterExponentialSampler(rng, 1);
        // Support future extension to input the cached log(n!) values.
        // Plain constructor uses the uncached function.
        factorialLog = NO_CACHE_FACTORIAL_LOG;

        // Cache values used in the algorithm
        lambda = Math.floor(mean);
        lambdaFractional = mean - lambda;
        logLambda = Math.log(lambda);
        logLambdaFactorial = factorialLog((int) lambda);
        delta = Math.sqrt(lambda * Math.log(32 * lambda / Math.PI + 1));
        halfDelta = delta / 2;
        twolpd = 2 * lambda + delta;
        c1 = 1 / (8 * lambda);
        final double a1 = Math.sqrt(Math.PI * twolpd) * Math.exp(c1);
        final double a2 = (twolpd / delta) * Math.exp(-delta * (1 + delta) / twolpd);
        final double aSum = a1 + a2 + 1;
        p1 = a1 / aSum;
        p2 = a2 / aSum;

        // The algorithm requires a Poisson sample from the lambda fraction
        smallMeanPoissonSampler = (lambdaFractional < Double.MIN_VALUE) ? null
                : new SmallMeanPoissonSampler(rng, lambdaFractional);
    }

    /**
     * Instantiates a sampler using a precomputed state.
     *
     * @param rng              Generator of uniformly distributed random numbers.
     * @param state            the state
     * @param lambdaFractional the lambda fractional value
     *                         ({@code 0 <= lambdaFractional < 1})
     * @throws IllegalArgumentException if
     *                                  {@code lambdaFractional < 0 || lambdaFractional >= 1}.
     */
    LargeMeanPoissonSampler(UniformRandomProvider rng, LargeMeanPoissonSamplerState state, double lambdaFractional) {
        super(rng);
        if (lambdaFractional < 0 || lambdaFractional >= 1) {
            throw new IllegalArgumentException(
                    "lambdaFractional must be in the range 0 (inclusive) to 1 (exclusive): " + lambdaFractional);
        }

        gaussian = new BoxMullerGaussianSampler(rng, 0, 1);
        exponential = new AhrensDieterExponentialSampler(rng, 1);
        // Support future extension to input the cached log(n!) values.
        // Plain constructor uses the uncached function.
        factorialLog = NO_CACHE_FACTORIAL_LOG;

        // Use the state to initialise the algorithm
        this.lambda = state.lambda;
        this.logLambda = state.logLambda;
        this.logLambdaFactorial = state.logLambdaFactorial;
        this.delta = state.delta;
        this.halfDelta = state.halfDelta;
        this.twolpd = state.twolpd;
        this.p1 = state.p1;
        this.p2 = state.p2;
        this.c1 = state.c1;
        this.lambdaFractional = lambdaFractional;

        // The algorithm requires a Poisson sample from the lambda fraction
        smallMeanPoissonSampler = (lambdaFractional < Double.MIN_VALUE) ? null
                : new SmallMeanPoissonSampler(rng, lambdaFractional);
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {

        // Move this to the end if this version does not have to match
        // the original PoissonSampler
        final int y2 = (smallMeanPoissonSampler == null) ? 0 : smallMeanPoissonSampler.sample();

        double x = 0;
        double y = 0;
        double v = 0;
        int a = 0;
        double t = 0;
        double qr = 0;
        double qa = 0;
        while (true) {
            final double u = nextDouble();
            if (u <= p1) {
                final double n = gaussian.sample();
                x = n * Math.sqrt(lambda + halfDelta) - 0.5d;
                if (x > delta || x < -lambda) {
                    continue;
                }
                y = x < 0 ? Math.floor(x) : Math.ceil(x);
                final double e = exponential.sample();
                v = -e - 0.5 * n * n + c1;
            } else {
                if (u > p1 + p2) {
                    y = lambda;
                    break;
                }
                x = delta + (twolpd / delta) * exponential.sample();
                y = Math.ceil(x);
                v = -exponential.sample() - delta * (x + 1) / twolpd;
            }
            a = x < 0 ? 1 : 0;
            t = y * (y + 1) / (2 * lambda);
            if (v < -t && a == 0) {
                y = lambda + y;
                break;
            }
            qr = t * ((2 * y + 1) / (6 * lambda) - 1);
            qa = qr - (t * t) / (3 * (lambda + a * (y + 1)));
            if (v < qa) {
                y = lambda + y;
                break;
            }
            if (v > qr) {
                continue;
            }
            if (v < y * logLambda - factorialLog((int) (y + lambda)) + logLambdaFactorial) {
                y = lambda + y;
                break;
            }
        }

        //// Do this if the return value does not have to match the old PoissonSampler
        // if (smallMeanPoissonSampler == null) {
        // // No small mean to sample
        // return (int) Math.min((long) y, Integer.MAX_VALUE);
        // }
        //
        // return (int) Math.min(smallMeanPoissonSampler.sample() + (long) y,
        //// Integer.MAX_VALUE);

        return (int) Math.min(y2 + (long) y, Integer.MAX_VALUE);
    }

    /**
     * Compute the natural logarithm of the factorial of {@code n}.
     *
     * @param n Argument.
     * @return {@code log(n!)}
     * @throws IllegalArgumentException if {@code n < 0}.
     */
    private final double factorialLog(int n) {
        return factorialLog.value(n);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Large Mean Poisson deviate [" + super.toString() + "]";
    }
}
