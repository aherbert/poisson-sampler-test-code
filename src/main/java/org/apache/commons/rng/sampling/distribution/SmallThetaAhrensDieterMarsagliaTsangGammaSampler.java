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
 *   For {@code 0 < theta < 1}:
 *   <blockquote>
 *    Ahrens, J. H. and Dieter, U.,
 *    <i>Computer methods for sampling from gamma, beta, Poisson and binomial distributions,</i>
 *    Computing, 12, 223-246, 1974.
 *   </blockquote>
 *  </li>
 * </ul>
 */
public class SmallThetaAhrensDieterMarsagliaTsangGammaSampler
    extends SamplerBase
    implements ContinuousSampler {
    /** The shape parameter. */
    private final double theta;
    /** The alpha parameter. */
    private final double alpha;
    /** Algorithm constant: {@code 1 + theta / Math.E} */
    private final double bGS;
    /** Algorithm constant: {@code 1 / theta} */
    private final double inverse_theta;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Alpha parameter of the distribution.
     * @param theta Theta parameter of the distribution (in the range {@code 0 < theta < 1}).
     * @throws IllegalArgumentException if {@code theta <= 0 || theta >= 1}.
     */
    public SmallThetaAhrensDieterMarsagliaTsangGammaSampler(UniformRandomProvider rng,
                                                  double alpha,
                                                  double theta) {
        super(rng);
        if (theta <= 0 || theta >= 1) {
            throw new IllegalArgumentException("Theta " + theta + " is not in the range: 0 < theta < 1");
        }
        this.alpha = alpha;
        this.theta = theta;
        bGS = 1 + theta / Math.E;
        inverse_theta = 1 / theta;
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        // [1]: p. 228, Algorithm GS.

        while (true) {
            // Step 1:
            final double u = nextDouble();
            final double p = bGS * u;

            if (p <= 1) {
                // Note: 
                // At the limit theta=0 this executes 1 / (1 + 0/E)
                // fraction of the time = 100%
                // At the limit theta=1 this executes 1 / (1 + 1/E)
                // fraction of the time = 73.1%

                // Step 2:

                final double x = Math.pow(p, inverse_theta);
                final double u2 = nextDouble();

                if (u2 > Math.exp(-x)) {
                    // Reject.
                    continue;
                }
                return alpha * x;
            }
            // Step 3:

            final double x = -1 * Math.log((bGS - p) / theta);
            final double u2 = nextDouble();

            if (u2 > Math.pow(x, theta - 1)) {
                // Reject.
                continue;
            }
            return alpha * x;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Small Theta Ahrens-Dieter-Marsaglia-Tsang Gamma deviate [" + super.toString() + "]";
    }
}
