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

import java.util.concurrent.TimeUnit;

import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Executes benchmark to compare the speed of generation of random numbers from
 * the various source providers.
 * <p>
 * This is adapted from
 * {@code org.apache.commons.rng.sampling.distribution.SamplersPerformance} in
 * the commons-rng-examples-jmh project.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class PoissonSamplersPerformance {
    /** Number of samples per run. */
    private static final int NUM_SAMPLES = 100000;

    /**
     * The benchmark state (retrieve the various "RandomSource"s).
     */
    @State(Scope.Benchmark)
    public static class Sources {
        /**
         * RNG providers.
         */
        @Param({ "WELL_19937_C", "WELL_44497_B", "SPLIT_MIX_64" })
        private String randomSourceName;

        /** RNG. */
        private RestorableUniformRandomProvider generator;

        /**
         * The state of the generator at the start of the test (for reproducible
         * results).
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
         * Test mean. Note the small mean sampler is used when mean is under 40.
         */
        @Param({ "5.3", "20.1", "35.7" })
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
         * Test mean. Note the PoissonSampler log(n!) cache goes from 40-80 so test the
         * extremes and the middle.
         */
        @Param({ "40.3", "60.9", "142.3" })
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
     * The range of mean values for testing the cache.
     */
    @State(Scope.Benchmark)
    public static class MeanRange {
        /**
         * Test range.
         */
        @Param({ "256", "1024" })
        private double range;

        private double[] mean;

        /**
         * Gets the mean.
         *
         * @param i the index
         * @return the mean
         */
        public double getMean(int i) {
            return getMin() + mean[i % mean.length] * range;
        }

        /**
         * Gets the min of the range.
         *
         * @return the min
         */
        public double getMin() {
            return WrapperPoissonSampler.PIVOT;
        }

        /**
         * Gets the max of the range.
         *
         * @return the max
         */
        public double getMax() {
            return getMin() + range;
        }

        /**
         * Create the samples from the range.
         */
        @Setup
        public void setup() {
            UniformRandomProvider generator = RandomSource.create(RandomSource.WELL_44497_B);
            // Ideally this should be large enough to fully sample the range 
            // when expressed as discrete integers, i.e. no sparseness.
            mean = new double[10000];
            for (int i = mean.length; i-- > 0;)
                mean[i] = generator.nextDouble();
        }
    }

    /**
     * A factory for creating DiscreteSampler objects.
     */
    @FunctionalInterface
    private interface DiscreteSamplerFactory {
        /**
         * Creates a new DiscreteSampler object.
         *
         * @return The discrete sampler
         */
        DiscreteSampler createDiscreteSampler();
    }

    /**
     * A factory for creating DiscreteSampler objects.
     */
    @FunctionalInterface
    private interface DiscreteSamplerFactoryWithMean {
        /**
         * Creates a new DiscreteSampler object.
         *
         * @param mean the mean
         * @return The discrete sampler
         */
        DiscreteSampler createDiscreteSampler(double mean);
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

    /**
     * Exercises a discrete sampler created for a single use.
     *
     * @param factory The factory.
     * @param bh      Data sink.
     */
    private static void runSample(DiscreteSamplerFactory factory, Blackhole bh) {
        for (int i = 0; i < NUM_SAMPLES; i++) {
            bh.consume(factory.createDiscreteSampler().sample());
        }
    }

    /**
     * Exercises a discrete sampler created for a single use.
     *
     * @param factory The factory.
     * @param range   the range
     * @param bh      Data sink.
     */
    private static void runSample(DiscreteSamplerFactoryWithMean factory, MeanRange range, Blackhole bh) {
        for (int i = 0; i < NUM_SAMPLES; i++) {
            bh.consume(factory.createDiscreteSampler(range.getMean(i)).sample());
        }
    }

    // Benchmarks methods below.

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallMeanRepeatUse_PoissonSampler(Sources sources, SmallMean mean, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(new PoissonSampler(r, mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallMeanRepeatUse_WrapperPoissonSampler(Sources sources, SmallMean mean, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(new WrapperPoissonSampler(r, mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallMeanRepeatUse_SmallMeanPoissonSampler(Sources sources, SmallMean mean, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(new SmallMeanPoissonSampler(r, mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeMeanRepeatUse_PoissonSampler(Sources sources, LargeMean mean, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(new PoissonSampler(r, mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeMeanRepeatUse_WrapperPoissonSampler(Sources sources, LargeMean mean, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(new WrapperPoissonSampler(r, mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeMeanRepeatUse_LargeMeanPoissonSampler(Sources sources, LargeMean mean, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(new LargeMeanPoissonSampler(r, mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallMeanSingleUse_PoissonSampler(Sources sources, SmallMean mean, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(() -> new PoissonSampler(r, mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallMeanSingleUse_WrapperPoissonSampler(Sources sources, SmallMean mean, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(() -> new WrapperPoissonSampler(r, mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallMeanSingleUse_SmallMeanPoissonSampler(Sources sources, SmallMean mean, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(() -> new SmallMeanPoissonSampler(r, mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeMeanSingleUse_PoissonSampler(Sources sources, LargeMean mean, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(() -> new PoissonSampler(r, mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeMeanSingleUse_WrapperPoissonSampler(Sources sources, LargeMean mean, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(() -> new WrapperPoissonSampler(r, mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param mean    the mean
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeMeanSingleUse_LargeMeanPoissonSampler(Sources sources, LargeMean mean, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(() -> new LargeMeanPoissonSampler(r, mean.getMean()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param range   the range
     * @param bh      Data sink.
     */
    @Benchmark
    public void runPoissonSamplerCache_NoCache(Sources sources, MeanRange range, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        final PoissonSamplerCache cache = new PoissonSamplerCache(0, 0);
        runSample((m) -> cache.getPoissonSampler(r, m), range, bh);
    }

    /**
     * @param sources Source of randomness.
     * @param range   the range
     * @param bh      Data sink.
     */
    @Benchmark
    public void runPoissonSamplerCache_SyncCache(Sources sources, MeanRange range, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        final PoissonSamplerCache cache = new PoissonSamplerCache(range.getMin(), range.getMax());
        runSample((m) -> cache.getPoissonSampler(r, m), range, bh);
    }
    
    /**
     * @param sources Source of randomness.
     * @param range   the range
     * @param bh      Data sink.
     */
    @Benchmark
    public void runPoissonSamplerCache_Cache(Sources sources, MeanRange range, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        final PoissonSamplerCache2 cache = new PoissonSamplerCache2(range.getMin(), range.getMax());
        runSample((m) -> cache.getPoissonSampler(r, m), range, bh);
    }
}
