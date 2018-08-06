/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Sampling from the <a href="http://mathworld.wolfram.com/GammaDistribution.html">Gamma distribution</a>.
 * <ul>
 *  <li>
 *  For {@code theta >= 1}:
 *   <blockquote>
 *   Marsaglia and Tsang, <i>A Simple Method for Generating
 *   Gamma Variables.</i> ACM Transactions on Mathematical Software,
 *   Volume 26 Issue 3, September, 2000.
 *   </blockquote>
 *  </li>
 * </ul>
 */
public class LargeThetaAhrensDieterMarsagliaTsangGammaSampler
    extends SamplerBase
    implements ContinuousSampler {
    /** Gaussian sampling. */
    private final BoxMullerGaussianSampler gaussian;
    /** Algorithm constant: {@code theta - 0.333333333333333333}*/
    private final double d;
    /** Algorithm constant: {@code 1 / (3 * Math.sqrt(d))}*/
    private final double c;
    /** Algorithm constant: {@code alpha * d}*/
    private final double alpha_by_d;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Alpha parameter of the distribution.
     * @param theta Theta parameter of the distribution.
     * @throws IllegalArgumentException if {@code theta < 1}.
    */
    public LargeThetaAhrensDieterMarsagliaTsangGammaSampler(UniformRandomProvider rng,
                                                  double alpha,
                                                  double theta) {
        super(rng);
        if (theta < 1) {
            throw new IllegalArgumentException("Theta " + theta + " < 1");
        }
        gaussian = new BoxMullerGaussianSampler(rng, 0, 1);
        d = theta - 0.333333333333333333;
        c = 1 / (3 * Math.sqrt(d));
        alpha_by_d = alpha * d;
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        while (true) {
            final double x = gaussian.sample();
            final double v = (1 + c * x) * (1 + c * x) * (1 + c * x);

            if (v <= 0) {
                continue;
            }

            final double x2 = x * x;
            final double u = nextDouble();

            // Squeeze.
            if (u < 1 - 0.0331 * x2 * x2) {
                return alpha_by_d * v;
            }

            if (Math.log(u) < 0.5 * x2 + d * (1 - v + Math.log(v))) {
                return alpha_by_d * v;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Large Theta Ahrens-Dieter-Marsaglia-Tsang Gamma deviate [" + super.toString() + "]";
    }
}
