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
import org.apache.commons.rng.sampling.PermutationSampler;
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
 * Executes benchmark to compare the speed of generation of Gamma random numbers
 * from the various source providers.
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
public class GammaSamplersPerformance {
    /** Number of samples per run. */
    private static final int NUM_SAMPLES = 100000;
    /**
     * The alpha parameter. This is just a scaling factor for the Gamma sample and
     * so is not varied in the benchmark.
     */
    private static final double ALPHA = 3.56757;

    /**
     * Seed used to ensure the tests are the same. This can be different per
     * benchmark, but should be the same within the benchmark.
     */
    private static final int[] seed;

    /**
     * The range sample. Ideally this should be large enough to fully sample the
     * range when expressed as discrete integers, i.e. no sparseness.
     */
    private static final double[] rangeSample;

    static {
        //
        seed = new int[128];
        UniformRandomProvider rng = RandomSource.create(RandomSource.WELL_44497_B);
        for (int i = seed.length; i-- > 0;)
            seed[i] = rng.nextInt();

        int size = 10000;
        int[] sample = PermutationSampler.natural(size);
        PermutationSampler.shuffle(rng, sample);

        rangeSample = new double[size];
        for (int i = 0; i < size; i++)
            rangeSample[i] = (double) sample[i] / size;
    }

    /**
     * The benchmark state (retrieve the various "RandomSource"s).
     */
    @State(Scope.Benchmark)
    public static class Sources {
        /**
         * RNG providers. Use different speeds.
         * 
         * @see <a href=
         *      "https://commons.apache.org/proper/commons-rng/userguide/rng.html">Commons
         *      RNG user guide</a>
         */
        @Param({ "SPLIT_MIX_64", "KISS", "WELL_1024_A", "WELL_44497_B" })
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
            // Use the same seed
            generator = RandomSource.create(randomSource, seed);
            state = generator.saveState();
        }
    }

    /**
     * The small theta value for testing.
     */
    @State(Scope.Benchmark)
    public static class SmallTheta {
        /**
         * Test theta. Note the small theta sampler is used when theta is under 1.
         */
        @Param({ "0.123", "0.51", "0.9876" })
        private double theta;

        /**
         * Gets the theta.
         *
         * @return the theta
         */
        public double getTheta() {
            return theta;
        }
    }

    /**
     * The large theta value for testing.
     */
    @State(Scope.Benchmark)
    public static class LargeTheta {
        /**
         * Test theta. Note the large theta sampler is used when theta is over 1.
         */
        @Param({ "2.456", "60.9" })
        private double theta;

        /**
         * Gets the theta.
         *
         * @return the theta
         */
        public double getTheta() {
            return theta;
        }
    }

    /**
     * A factory for creating ContinuousSampler objects.
     */
    @FunctionalInterface
    private interface ContinuousSamplerFactory {
        /**
         * Creates a new ContinuousSampler object.
         *
         * @return The discrete sampler
         */
        ContinuousSampler createContinuousSampler();
    }

    /**
     * A factory for creating ContinuousSampler objects.
     */
    @FunctionalInterface
    private interface ContinuousSamplerFactoryWithTheta {
        /**
         * Creates a new ContinuousSampler object.
         *
         * @param theta the theta
         * @return The discrete sampler
         */
        ContinuousSampler createContinuousSampler(double theta);
    }

    /**
     * Exercises a discrete sampler.
     *
     * @param sampler Sampler.
     * @param bh      Data sink.
     */
    private static void runSample(ContinuousSampler sampler, Blackhole bh) {
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
    private static void runSample(ContinuousSamplerFactory factory, Blackhole bh) {
        for (int i = 0; i < NUM_SAMPLES; i++) {
            bh.consume(factory.createContinuousSampler().sample());
        }
    }

    // Benchmarks methods below.

    /**
     * @param sources Source of randomness.
     * @param theta   the theta
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallThetaRepeatUse_AhrensDieterMarsagliaTsangGammaSampler(Sources sources, SmallTheta theta,
            Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(new AhrensDieterMarsagliaTsangGammaSampler(r, ALPHA, theta.getTheta()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param theta   the theta
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallThetaRepeatUse_WrapperAhrensDieterMarsagliaTsangGammaSampler(Sources sources, SmallTheta theta,
            Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(new WrapperAhrensDieterMarsagliaTsangGammaSampler(r, ALPHA, theta.getTheta()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param theta   the theta
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallThetaRepeatUse_SmallThetaAhrensDieterMarsagliaTsangGammaSampler(Sources sources,
            SmallTheta theta, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(new SmallThetaAhrensDieterMarsagliaTsangGammaSampler(r, ALPHA, theta.getTheta()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param theta   the theta
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeThetaRepeatUse_AhrensDieterMarsagliaTsangGammaSampler(Sources sources, LargeTheta theta,
            Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(new AhrensDieterMarsagliaTsangGammaSampler(r, ALPHA, theta.getTheta()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param theta   the theta
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeThetaRepeatUse_WrapperAhrensDieterMarsagliaTsangGammaSampler(Sources sources, LargeTheta theta,
            Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(new WrapperAhrensDieterMarsagliaTsangGammaSampler(r, ALPHA, theta.getTheta()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param theta   the theta
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeThetaRepeatUse_LargeThetaAhrensDieterMarsagliaTsangGammaSampler(Sources sources,
            LargeTheta theta, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(new LargeThetaAhrensDieterMarsagliaTsangGammaSampler(r, ALPHA, theta.getTheta()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param theta   the theta
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallThetaSingleUse_AhrensDieterMarsagliaTsangGammaSampler(Sources sources, SmallTheta theta,
            Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(() -> new AhrensDieterMarsagliaTsangGammaSampler(r, ALPHA, theta.getTheta()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param theta   the theta
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallThetaSingleUse_WrapperAhrensDieterMarsagliaTsangGammaSampler(Sources sources, SmallTheta theta,
            Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(() -> new WrapperAhrensDieterMarsagliaTsangGammaSampler(r, ALPHA, theta.getTheta()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param theta   the theta
     * @param bh      Data sink.
     */
    @Benchmark
    public void runSmallThetaSingleUse_SmallThetaAhrensDieterMarsagliaTsangGammaSampler(Sources sources,
            SmallTheta theta, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(() -> new SmallThetaAhrensDieterMarsagliaTsangGammaSampler(r, ALPHA, theta.getTheta()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param theta   the theta
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeThetaSingleUse_AhrensDieterMarsagliaTsangGammaSampler(Sources sources, LargeTheta theta,
            Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(() -> new AhrensDieterMarsagliaTsangGammaSampler(r, ALPHA, theta.getTheta()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param theta   the theta
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeThetaSingleUse_WrapperAhrensDieterMarsagliaTsangGammaSampler(Sources sources, LargeTheta theta,
            Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(() -> new WrapperAhrensDieterMarsagliaTsangGammaSampler(r, ALPHA, theta.getTheta()), bh);
    }

    /**
     * @param sources Source of randomness.
     * @param theta   the theta
     * @param bh      Data sink.
     */
    @Benchmark
    public void runLargeThetaSingleUse_LargeThetaAhrensDieterMarsagliaTsangGammaSampler(Sources sources,
            LargeTheta theta, Blackhole bh) {
        final UniformRandomProvider r = sources.getGenerator();
        runSample(() -> new LargeThetaAhrensDieterMarsagliaTsangGammaSampler(r, ALPHA, theta.getTheta()), bh);
    }
}
