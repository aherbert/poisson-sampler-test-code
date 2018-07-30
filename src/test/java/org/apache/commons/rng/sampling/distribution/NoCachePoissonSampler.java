package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.InternalUtils.FactorialLog;

/**
 * This is a new implementation of the {@link PoissonSampler} modified to remove
 * the use of the cache.
 */
public class NoCachePoissonSampler extends SamplerBase implements DiscreteSampler {
    /** Value for switching sampling algorithm. */
    static final double PIVOT = 40;
    /** The internal Poisson sampler. */
    private final DiscreteSampler poissonSampler;

    /** {@code log(n!)}. */
    private static final InternalUtils.FactorialLog factorialLog;

    static {
	// Do not cache any log(n!) values.
	// Just use this class to get the log(n!) values.
	// This makes the results exactly match the current PoissonSampler.
	factorialLog = FactorialLog.create(); //.withCache((int)(2 * PIVOT));
    }

    /**
     * For small means, a Poisson process is simulated using uniform deviates, as
     * described
     * <a href="http://mathaa.epfl.ch/cours/PMMI2001/interactive/rng7.htm">here</a>.
     * The Poisson process (and hence, the returned value) is bounded by 1000 *
     * mean.
     */
    private class SmallMeanPoissonSampler implements DiscreteSampler {

	final double meanPoisson;
	final double p;

	SmallMeanPoissonSampler(double mean) {
	    // Unchecked argument
	    meanPoisson = mean;
	    p = Math.exp(-meanPoisson);
	}

	/** {@inheritDoc} */
	public int sample() {
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
	    return (n < Integer.MAX_VALUE) ? (int) n : Integer.MAX_VALUE;
	}
    }

    /**
     * For the special case where the lambdaFractional is too small for a Poisson
     * sample this sampler is used to return zero.
     */
    private class NoPoissonSampler implements DiscreteSampler {
	/** {@inheritDoc} */
	public int sample() {
	    return 0;
	}
    }

    /**
     * For large means, we use the rejection algorithm described in <blockquote>
     * Devroye, Luc. (1981).<i>The Computer Generation of Poisson Random
     * Variables</i><br>
     * <strong>Computing</strong> vol. 26 pp. 197-207. </blockquote>
     */
    private class BigMeanPoissonSampler implements DiscreteSampler {

	final double meanPoisson;

	/** Exponential. */
	private final ContinuousSampler exponential;
	/** Gaussian. */
	private final ContinuousSampler gaussian;

	// Working values
	final double lambda;
	final double lambdaFractional;
	final double logLambda;
	final double logLambdaFactorial;
	final double delta;
	final double halfDelta;
	final double twolpd;
	final double p1;
	final double p2;
	final double c1;

	/** The internal Poisson sampler for the lambda fraction. */
	private final DiscreteSampler poissonSampler;

	BigMeanPoissonSampler(UniformRandomProvider rng, double mean) {
	    // Unchecked arguments
	    meanPoisson = mean;

	    gaussian = new BoxMullerGaussianSampler(rng, 0, 1);
	    exponential = new AhrensDieterExponentialSampler(rng, 1);

	    // Cache values used in the algorithm
	    lambda = Math.floor(meanPoisson);
	    lambdaFractional = meanPoisson - lambda;
	    logLambda = Math.log(lambda);
	    logLambdaFactorial = factorialLog((int) lambda);
	    delta = Math.sqrt(lambda * Math.log(32 * lambda / Math.PI + 1));
	    halfDelta = delta / 2;
	    twolpd = 2 * lambda + delta;
	    final double a1 = Math.sqrt(Math.PI * twolpd) * Math.exp(1 / (8 * lambda));
	    final double a2 = (twolpd / delta) * Math.exp(-delta * (1 + delta) / twolpd);
	    final double aSum = a1 + a2 + 1;
	    p1 = a1 / aSum;
	    p2 = a2 / aSum;
	    c1 = 1 / (8 * lambda);

	    poissonSampler = lambdaFractional < Double.MIN_VALUE ? new NoPoissonSampler()
		    : new SmallMeanPoissonSampler(mean);
	}

	/** {@inheritDoc} */
	public int sample() {

	    final long y2 = poissonSampler.sample();

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
		if (v < y * logLambda - factorialLog((int) (y + lambda)) + logLambdaFactorial) {
		    y = lambda + y;
		    break;
		}
	    }
	    return (int) Math.min(y2 + (long) y, Integer.MAX_VALUE);
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
    }

    /**
     * @param rng  Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    public NoCachePoissonSampler(UniformRandomProvider rng, double mean) {
	super(rng);
	if (mean <= 0) {
	    throw new IllegalArgumentException(mean + " <= " + 0);
	}
	// Delegate all work to internal samplers
	poissonSampler = mean < PIVOT ? new SmallMeanPoissonSampler(mean) : new BigMeanPoissonSampler(rng, mean);
    }

    /** {@inheritDoc} */
    public int sample() {
	return poissonSampler.sample();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
	return "Poisson deviate [" + super.toString() + "]";
    }
}