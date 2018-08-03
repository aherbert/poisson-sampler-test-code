package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Assert;
import org.junit.Test;

import gnu.trove.list.array.TDoubleArrayList;

/**
 * This test check the speed of constructing a PoissonSampler inside a loop
 */
public class PoissonSamplerTest {
    /**
     * This tests the speed of constructing a PoissonSampler inside a loop
     * verses outside.
     */
    @Test
    public void canComputePoissonSampleWithStaticFactorialLog() {
        final Long seed = 768348678L;
        final RestorableUniformRandomProvider rng = RandomSource
                .create(RandomSource.WELL_19937_C, seed);
        final RandomProviderState state = rng.saveState();

        // Warm-up RNG
        final int LOOPS = 10000;
        for (int i = 0; i < LOOPS; i++) {
            rng.nextDouble();
            rng.nextDouble();
            rng.nextDouble();
        }

        final int[] data1 = new int[LOOPS];
        final int[] data2 = new int[LOOPS];
        final int[] data3 = new int[LOOPS];

        for (int n = 10; n <= 80; n += 5) {
            final double mean = n + 0.3;
            // Time the default sampler constructed inside a loop
            rng.restoreState(state);

            long start = System.nanoTime();
            for (int i = 0; i < LOOPS; i++)
                data1[i] = new DefaultPoissonSampler(rng, mean).sample();
            final long loopTime = System.nanoTime() - start;

            // Time the default sampler constructed outside a loop
            rng.restoreState(state);

            start = System.nanoTime();
            final DefaultPoissonSampler ps = new DefaultPoissonSampler(rng,
                    mean);
            for (int i = 0; i < LOOPS; i++) {
                ps.resetGaussian(); // This ensure the random numbers are the
                                    // same
                data2[i] = ps.sample();
            }
            final long singleTime = System.nanoTime() - start;

            // Time the static cache sampler constructed inside a loop
            rng.restoreState(state);

            start = System.nanoTime();
            for (int i = 0; i < LOOPS; i++)
                data3[i] = new FixedCachePoissonSampler(rng, mean).sample();
            final long fastLoopTime = System.nanoTime() - start;

            System.out.printf(
                    "Mean %5.1f  Single construction (%8d) vs Loop construction                          (%8d)   (%f.2x faster)\n",
                    mean, singleTime, loopTime, (double) loopTime / singleTime);
            System.out.printf(
                    "Mean %5.1f  Single construction (%8d) vs Loop construction with static FactorialLog (%8d)   (%f.2x faster)\n",
                    mean, singleTime, fastLoopTime,
                    (double) fastLoopTime / singleTime);
            Assert.assertArrayEquals("Random samples are not the same", data1,
                    data2);
            Assert.assertArrayEquals("Random samples are not the same", data1,
                    data3);
            // Assertions.assertTrue(loopTime > singleTime * 2, "Construction
            // cost inside
            // loop causes >2x slowdown");
        }
    }

    /**
     * This tests the histogram functions as expected
     */
    @Test
    public void canHistogramData() {
        // Empty
        final IntegerHistogram h = new IntegerHistogram(10);
        checkStats(new int[0][0], h);

        // Fill inside and outside cache range
        h.add(3);
        h.add(2);
        h.add(1);
        h.add(15);
        h.add(3);
        h.add(5);
        checkStats(new int[][] { { 1, 2, 3, 5, 15 }, { 1, 1, 2, 1, 1 }, }, h);

        // Empty
        h.clear();
        checkStats(new int[0][0], h);

        // Fill outside cache range
        h.add(21);
        h.add(15);
        h.add(20);
        checkStats(new int[][] { { 15, 20, 21 }, { 1, 1, 1 }, }, h);

        // Fill inside cache range
        h.clear();
        h.add(2);
        h.add(1);
        h.add(3);
        checkStats(new int[][] { { 1, 2, 3 }, { 1, 1, 1 }, }, h);
    }

    private static void checkStats(int[][] histogram, IntegerHistogram h) {
        final int[][] o = h.getHistogram();
        final double[] stats = h.getStatistics();

        if (histogram.length == 0) {
            Assert.assertNull(stats);
            return;
        }

        Assert.assertArrayEquals(histogram, o);

        final SummaryStatistics s = new SummaryStatistics();
        final int[] value = histogram[0];
        final int[] count = histogram[1];
        for (int i = 0; i < value.length; i++)
            for (int j = count[i]; j-- > 0;)
                s.addValue(value[i]);

        final double[] e = new double[] { s.getMin(), s.getMax(), s.getMean(),
                s.getStandardDeviation() };

        Assert.assertEquals("Min", e[0], stats[0], 0);
        Assert.assertEquals("Max", e[1], stats[1], 0);
        Assert.assertEquals("Av.", e[2], stats[2], e[2] * 1e-8);
        Assert.assertEquals("SD.", e[3], stats[3], e[3] * 1e-8);
    }

    /**
     * This records the usage of the {@code log(n!)} function.
     */
    @Test
    public void canRecordLogFactorialUsage() {
        final Long seed = 768348678L;
        final RestorableUniformRandomProvider rng = RandomSource
                .create(RandomSource.WELL_19937_C, seed);
        // The maximum mean to test.
        // Note: The minimum is 40 (the minimum mean where this sampling method
        // is
        // used).
        int max = 100;
        // The number of Poisson samples.
        // This is an upper limit to prevent excess sampling.
        final int N_SAMPLES = 2000000;
        // The number of log(n!) calls
        final int N_CALLS = 100000;

        // Get the means for testing
        final TDoubleArrayList list = new TDoubleArrayList();
        for (int mean = 40; mean <= max; mean += 5)
            list.add(mean + 0.3);
        for (int i = 0; i < 10; i++) {
            max *= 2;
            list.add(max + 0.3);
        }

        final IntegerHistogram h = new IntegerHistogram(4096);

        final StringBuilder sb = new StringBuilder();
        sb.append(
                "||Mean||Samples||log(n!) calls||Calls/sample||min n||max n||Av n||SD n||Poisson SD||\n");

        for (final double mean : list.toArray()) {
            h.clear();
            int samples = 0;
            final RecordingPoissonSampler ps = new RecordingPoissonSampler(rng,
                    mean, h);
            while (samples < N_SAMPLES) {
                samples++;
                ps.sample();
                // If we have a lot of data in the histogram then stop
                if (h.getCount() > N_CALLS)
                    break;
            }

            // Report
            final double[] stats = h.getStatistics();
            final double callPerSample = (double) h.getCount() / samples;
            final int minN = (int) stats[0];
            final int maxN = (int) stats[1];
            final double av = stats[2];
            final double sd = stats[3];
            final double poissonSD = Math.sqrt(mean);
            System.out.printf(
                    "Mean %5.1f  Samples=%7d  log(n!)=%6d   (%8.3g / sample)  min=%6d  max=%6d  mean=%10.3f  SD=%7.3f   Poisson SD=%7.3f\n",
                    mean, samples, h.getCount(), callPerSample, minN, maxN, av,
                    sd, poissonSD);
            sb.append('|').append(round1DP(mean));
            sb.append('|').append(samples);
            sb.append('|').append(h.getCount());
            sb.append('|').append(String.format("%8.3g", callPerSample));
            sb.append('|').append(minN);
            sb.append('|').append(maxN);
            sb.append('|').append(round1DP(av));
            sb.append('|').append(round1DP(sd));
            sb.append('|').append(round1DP(poissonSD));
            sb.append("|\n");
        }

        System.out.println("---");
        System.out.print(sb.toString());
        System.out.println("---");
    }

    private static String round1DP(double d) {
        return String.format("%.1f", d);
    }

    /**
     * This tests the speed of the default PoissonSampler and a new
     * implementation with no cache of the {@code log(n!)} function inside a
     * loop verses outside.
     */
    @Test
    public void canComputeFastPoissonSamples() {
        final Long seed = 768348678L;
        final RestorableUniformRandomProvider rng = RandomSource
                .create(RandomSource.WELL_19937_C, seed);
        final RandomProviderState state = rng.saveState();

        // Warm-up RNG
        final int LOOPS = 100000;
        for (int i = 0; i < LOOPS; i++) {
            rng.nextDouble();
            rng.nextDouble();
            rng.nextDouble();
        }

        final int[] data1 = new int[LOOPS];
        final int[] data2 = new int[LOOPS];
        final int[] data3 = new int[LOOPS];
        final int[] data4 = new int[LOOPS];

        final StringBuilder sb = new StringBuilder();
        sb.append(
                "||Mean||Samples||PoissonSampler||PoissonSampler single-use||Relative||NoCachePoissonSampler||Relative||NoCachePoissonSampler single-use||Relative||\n");

        for (int n = 10; n <= 80; n += 5) {
            final double mean = n + 0.3;
            // Time the default sampler constructed inside a loop
            rng.restoreState(state);

            long start = System.nanoTime();
            for (int i = 0; i < LOOPS; i++)
                data1[i] = new PoissonSampler(rng, mean).sample();
            final long loopTime = System.nanoTime() - start;

            // Time the default sampler constructed outside a loop
            rng.restoreState(state);

            start = System.nanoTime();
            final PoissonSampler ps = new PoissonSampler(rng, mean);
            for (int i = 0; i < LOOPS; i++)
                data2[i] = ps.sample();
            final long singleTime = System.nanoTime() - start;

            // Time the no cache sampler constructed inside a loop
            rng.restoreState(state);

            start = System.nanoTime();
            for (int i = 0; i < LOOPS; i++)
                data3[i] = new WrapperPoissonSampler(rng, mean).sample();
            final long loopTime2 = System.nanoTime() - start;

            // Time the no cache sampler constructed outside a loop
            rng.restoreState(state);

            start = System.nanoTime();
            final WrapperPoissonSampler ps2 = new WrapperPoissonSampler(rng,
                    mean);
            for (int i = 0; i < LOOPS; i++)
                data4[i] = ps2.sample();
            final long singleTime2 = System.nanoTime() - start;

            System.out.printf(
                    "Mean %5.1f  Single construction (%8d) vs Loop construction                          (%8d)   (%f.2x faster)\n",
                    mean, singleTime, loopTime, (double) loopTime / singleTime);
            System.out.printf(
                    "Mean %5.1f  Single construction (%8d) vs Loop construction with no cache            (%8d)   (%f.2x faster)\n",
                    mean, singleTime, loopTime2,
                    (double) loopTime2 / singleTime);
            System.out.printf(
                    "Mean %5.1f  Single construction (%8d) vs Single construction with no cache          (%8d)   (%f.2x faster)\n",
                    mean, singleTime, singleTime2,
                    (double) singleTime2 / singleTime);
            Assert.assertArrayEquals("Random samples are not the same", data1,
                    data3);
            Assert.assertArrayEquals("Random samples are not the same", data2,
                    data4);
            // Assertions.assertTrue(loopTime > singleTime * 2, "Construction
            // cost inside
            // loop causes >2x slowdown");

            sb.append('|').append(round1DP(mean));
            sb.append('|').append(LOOPS);
            sb.append('|').append(singleTime);
            sb.append('|').append(loopTime);
            sb.append('|').append(round3DP((double) loopTime / singleTime));
            sb.append('|').append(singleTime2);
            sb.append('|').append(round3DP((double) singleTime2 / singleTime));
            sb.append('|').append(loopTime2);
            sb.append('|').append(round3DP((double) loopTime2 / singleTime));
            sb.append("|\n");
        }

        System.out.println("---");
        System.out.print(sb.toString());
        System.out.println("---");
    }

    private static String round3DP(double d) {
        return String.format("%.3f", d);
    }
}
