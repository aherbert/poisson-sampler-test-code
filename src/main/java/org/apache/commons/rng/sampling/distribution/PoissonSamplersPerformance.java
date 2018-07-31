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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;
import java.util.concurrent.TimeUnit;

import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
import org.apache.commons.rng.sampling.distribution.PoissonSampler;

/**
 * Executes benchmark to compare the speed of generation of random numbers from
 * the various source providers.
 * <p>
 * This is adapted from
 * {@link org.apache.commons.rng.sampling.distribution.PoissonSamplersPerformance}
 * in the commons-rng-examples-jmh project.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class PoissonSamplersPerformance {
    /** Number of samples per run. */
    private static final int NUM_SAMPLES = 1000000;

/**
 * The benchmark state (retrieve the various "RandomSource"s).
 */
@State(Scope.Benchmark)
public static class Sources {
    /**
     * RNG providers.
     */
    @Param({"WELL_19937_C",
            "WELL_44497_B",
            "SPLIT_MIX_64"
           })
    private String randomSourceName;

    /** RNG. */
    private RestorableUniformRandomProvider generator;

    /**
     * The state of the generator at the start of the test (for reproducible results).
     */
    private RandomProviderState state;

    /**
     * @return the RNG.
     */
    public UniformRandomProvider getGenerator() {
        generator.restoreState(state);
        return generator;
    }

    /** Instantiates generator. */
    @Setup
    public void setup() {
        final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
        generator = RandomSource.create(randomSource);
        state = generator.saveState();
    }
}

    /**
     * The small mean value for testing.
     */
    @State(Scope.Benchmark)
    public static class SmallMean {
        /**
         * Test mean.
         */
        @Param({ "5.3", "8.5", "35.7" })
        private double mean;

        /**
         * Gets the mean.
         *
         * @return the mean
         */
        public double getMean() {
            return mean;
        }
    }

    /**
     * The large mean value for testing.
     */
    @State(Scope.Benchmark)
    public static class LargeMean {
        /**
         * Test mean.
         */
        @Param({ "40.3", "57.9", "142.3" })
        private double mean;

        /**
         * Gets the mean.
         *
         * @return the mean
         */
        public double getMean() {
            return mean;
        }
    }

    /**
     * Exercises a discrete sampler.
     *
     * @param sampler Sampler.
     * @param bh      Data sink.
     */
    private static void runSample(DiscreteSampler sampler, Blackhole bh) {
        for (int i = 0; i < NUM_SAMPLES; i++) {
            bh.consume(sampler.sample());
        }
    }

    // Benchmarks methods below.

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallMean_PoissonSampler(Sources sources, SmallMean mean, Blackhole bh) {
        runSample(new PoissonSampler(sources.getGenerator(), mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallMean_WrapperPoissonSampler(Sources sources, SmallMean mean, Blackhole bh) {
        runSample(new WrapperPoissonSampler(sources.getGenerator(), mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallMean_SmallMeanPoissonSampler(Sources sources, SmallMean mean, Blackhole bh) {
        runSample(new SmallMeanPoissonSampler(sources.getGenerator(), mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeMean_PoissonSampler(Sources sources, LargeMean mean, Blackhole bh) {
        runSample(new PoissonSampler(sources.getGenerator(), mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeMean_WrapperPoissonSampler(Sources sources, LargeMean mean, Blackhole bh) {
        runSample(new WrapperPoissonSampler(sources.getGenerator(), mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeMean_LargeMeanPoissonSampler(Sources sources, LargeMean mean, Blackhole bh) {
        runSample(new LargeMeanPoissonSampler(sources.getGenerator(), mean.getMean()), bh);
    }
}
