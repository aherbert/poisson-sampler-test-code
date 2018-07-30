package org.apache.commons.rng.sampling.distribution;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.InternalUtils.FactorialLog;

/**
 * This is a copy of the {@link PoissonSampler} modified so that the function
 * calls to compute {@code log(n!)} can be recorded.
 */
public class RecordingPoissonSampler
extends SamplerBase
implements DiscreteSampler {
    /** Value for switching sampling algorithm. */
    static final double PIVOT = 40;
    /** Mean of the distribution. */
    private final double mean;
    /** Exponential. */
    private final ContinuousSampler exponential;
    /** Gaussian. */
    private final ContinuousSampler gaussian;
    /** {@code log(n!)}. */
    private final FactorialLog factorialLog;
    /** Histogram to record calls to {@code log(n!)}. */ 
    private final IntegerHistogram histogram;
    
    /**
     * Instantiates a new recording poisson sampler.
     *
     * @param rng       Generator of uniformly distributed random numbers.
     * @param mean      Mean.
     * @param histogram the histogram
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    public RecordingPoissonSampler(UniformRandomProvider rng,
                          double mean, IntegerHistogram histogram) {
        super(rng);
        if (mean <= 0) {
            throw new IllegalArgumentException(mean + " <= " + 0);
        }

        this.mean = mean;

        gaussian = new BoxMullerGaussianSampler(rng, 0, 1);
        exponential = new AhrensDieterExponentialSampler(rng, 1);
        factorialLog = mean < PIVOT ?
            null : // Not used.
            FactorialLog.create().withCache((int) Math.min(mean, 2 * PIVOT));
        
        this.histogram = histogram;
    }

    /** {@inheritDoc} */
    public int sample() {
        return (int) Math.min(nextPoisson(mean), Integer.MAX_VALUE);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Poisson deviate [" + super.toString() + "]";
    }

    /**
     * @param meanPoisson Mean.
     * @return the next sample.
     */
    private long nextPoisson(double meanPoisson) {
        if (meanPoisson < PIVOT) {
            double p = Math.exp(-meanPoisson);
            long n = 0;
            double r = 1;

            while (n < 1000 * meanPoisson) {
                r *= nextDouble();
                if (r >= p) {
                    n++;
                } else {
                    break;
                }
            }
            return n;
        } else {
            final double lambda = Math.floor(meanPoisson);
            final double lambdaFractional = meanPoisson - lambda;
            final double logLambda = Math.log(lambda);
            
            // Do not record this as it is constant and could be precomputed
            final double logLambdaFactorial = factorialLog((int) lambda);
            
            final long y2 = lambdaFractional < Double.MIN_VALUE ? 0 : nextPoisson(lambdaFractional);
            final double delta = Math.sqrt(lambda * Math.log(32 * lambda / Math.PI + 1));
            final double halfDelta = delta / 2;
            final double twolpd = 2 * lambda + delta;
            final double a1 = Math.sqrt(Math.PI * twolpd) * Math.exp(1 / (8 * lambda));
            final double a2 = (twolpd / delta) * Math.exp(-delta * (1 + delta) / twolpd);
            final double aSum = a1 + a2 + 1;
            final double p1 = a1 / aSum;
            final double p2 = a2 / aSum;
            final double c1 = 1 / (8 * lambda);

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
                    } else {
                        x = delta + (twolpd / delta) * exponential.sample();
                        y = Math.ceil(x);
                        v = -exponential.sample() - delta * (x + 1) / twolpd;
                    }
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
                if (v < y * logLambda - recordFactorialLog((int) (y + lambda)) + logLambdaFactorial) {
                    y = lambda + y;
                    break;
                }
            }
            return y2 + (long) y;
        }
    }

    /**
     * Compute the natural logarithm of the factorial of {@code n}.
     *
     * @param n Argument.
     * @return {@code log(n!)}
     * @throws IllegalArgumentException if {@code n < 0}.
     */
    private double factorialLog(int n) {
        return factorialLog.value(n);
    }

    /**
     * Compute the natural logarithm of the factorial of {@code n}.
     *
     * @param n Argument.
     * @return {@code log(n!)}
     * @throws IllegalArgumentException if {@code n < 0}.
     */
    private double recordFactorialLog(int n) {
    	histogram.add(n);
        return factorialLog.value(n);
    }
}