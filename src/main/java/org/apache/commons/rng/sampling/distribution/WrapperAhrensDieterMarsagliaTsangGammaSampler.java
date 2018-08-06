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
public class WrapperAhrensDieterMarsagliaTsangGammaSampler
    implements ContinuousSampler {
    /** Gamma sampling. */
    private final ContinuousSampler gammaSampler;

    /**
     * @param rng Generator of uniformly distributed random numbers.
     * @param alpha Alpha parameter of the distribution.
     * @param theta Theta parameter of the distribution.
     */
    public WrapperAhrensDieterMarsagliaTsangGammaSampler(UniformRandomProvider rng,
                                                  double alpha,
                                                  double theta) {
        gammaSampler = theta < 1 
                ? new SmallThetaAhrensDieterMarsagliaTsangGammaSampler(rng, alpha, theta) 
                : new LargeThetaAhrensDieterMarsagliaTsangGammaSampler(rng, alpha, theta);
    }

    /** {@inheritDoc} */
    @Override
    public double sample() {
        return gammaSampler.sample();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Ahrens-Dieter-Marsaglia-Tsang Gamma deviate [" + super.toString() + "]";
    }
}
