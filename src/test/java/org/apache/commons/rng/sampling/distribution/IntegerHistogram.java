package org.apache.commons.rng.sampling.distribution;

import java.util.Arrays;
import java.util.Comparator;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.procedure.TIntProcedure;

/**
 * This allows accumulation of an integer histogram.
 */
public class IntegerHistogram {

    private int count = 0;
    private final int[] h;
    private final TIntIntHashMap extra = new TIntIntHashMap(1024);

    /**
     * Instantiates a new integer histogram with the specified cache size
     *
     * @param cacheSize the cache size
     */
    public IntegerHistogram(int cacheSize) {
        if (cacheSize < 1)
            throw new IllegalArgumentException("Unsupported cache size: " + cacheSize);
        h = new int[cacheSize];
    }

    /**
     * Adds the value to the histogram.
     *
     * @param n the value
     */
    public void add(int n) {
        count++;
        if (n < h.length) {
            h[n]++;
        } else {
            extra.adjustOrPutValue(n, 1, 1);
        }
    }

    /**
     * Clear the histogram.
     */
    public void clear() {
        count = 0;
        Arrays.fill(h, 0);
        extra.clear();
    }

    /**
     * Gets the histogram as a set of values and frequencies.
     *
     * @return the histogram [values,frequencies]
     */
    public int[][] getHistogram() {
        TIntArrayList v = new TIntArrayList(h.length);
        TIntArrayList f = new TIntArrayList(h.length);
        for (int i = 0; i < h.length; i++) {
            if (h[i] != 0) {
                v.add(i);
                f.add(h[i]);
            }
        }
        if (!extra.isEmpty()) {
            // Sort the entries in the hash map of extra counts
            final int[][] toSort = new int[extra.size()][];
            extra.forEachEntry(new TIntIntProcedure() {
                int i = 0;

                public boolean execute(int n, int count) {
                    toSort[i++] = new int[] { n, count };
                    return true;
                }
            });
            Arrays.sort(toSort, new Comparator<int[]>() {
                public int compare(int[] o1, int[] o2) {
                    return Integer.compare(o1[0], o2[0]);
                }
            });
            for (int[] pair : toSort) {
                v.add(pair[0]);
                f.add(pair[1]);
            }
        }
        return new int[][] { v.toArray(), f.toArray() };
    }

    /**
     * Gets the count.
     *
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * Gets the statistics [min,max,mean,sd].
     *
     * @return the statistics
     */
    public double[] getStatistics() {
        if (count == 0)
            return null;
        int min = 0;
        while (min < h.length && h[min] == 0)
            min++;

        int max;
        if (extra.isEmpty()) {
            // No extra values so the max is in the fixed histogram
            max = h.length - 1;
            while (h[max] == 0 && max > 0)
                max--;
        } else {
            // The max will be in the extra values
            final int[] tmp = new int[1];
            extra.forEachKey(new TIntProcedure() {
                public boolean execute(int value) {
                    if (tmp[0] < value)
                        tmp[0] = value;
                    return true;
                }
            });
            max = tmp[0];

            // There may only be extra values so the min must be computed
            if (min == h.length) {
                extra.forEachKey(new TIntProcedure() {
                    public boolean execute(int value) {
                        if (tmp[0] > value)
                            tmp[0] = value;
                        return true;
                    }
                });
                min = tmp[0];
            }
        }

        // Compute the sum and sum-of-squares for the standard deviation.
        if ((double) max * max * count < Long.MAX_VALUE) {
            // This assumes that longs will not overflow on the sum-of-squares
            long s = 0, ss = 0;
            for (int n = 0; n < h.length; n++) {
                if (h[n] != 0) {
                    long c_by_n = h[n] * (long) n;
                    s += c_by_n;
                    ss += c_by_n * n;
                }
            }
            if (!extra.isEmpty()) {
                final long[] tmp = new long[2];
                extra.forEachEntry(new TIntIntProcedure() {
                    public boolean execute(int n, int count) {
                        long c_by_n = count * (long) n;
                        tmp[0] += c_by_n;
                        tmp[1] += c_by_n * n;
                        return true;
                    }
                });
                s += tmp[0];
                ss += tmp[1];
            }

            double mean = (double) s / count;
            double secondMoment = ss - ((double) s * s) / count;
            double stdDev = (secondMoment > 0.0) ? Math.sqrt(secondMoment / (count - 1)) : 0;

            return new double[] { min, max, mean, stdDev };
        }

        // This assumes that longs will overflow on the sum-of-squares
        long s = 0;
        double ss = 0;
        for (int n = 0; n < h.length; n++) {
            if (h[n] != 0) {
                long c_by_n = h[n] * (long) n;
                s += c_by_n;
                ss += c_by_n * n;
            }
        }
        if (!extra.isEmpty()) {
            final long[] tmp1 = new long[1];
            final double[] tmp2 = new double[1];
            extra.forEachEntry(new TIntIntProcedure() {
                public boolean execute(int n, int count) {
                    long c_by_n = count * (long) n;
                    tmp1[0] += c_by_n;
                    tmp2[0] += c_by_n * n;
                    return true;
                }
            });
            s += tmp1[0];
            ss += tmp2[0];
        }

        double mean = (double) s / count;
        double secondMoment = ss - ((double) s * s) / count;
        double stdDev = (secondMoment > 0.0) ? Math.sqrt(secondMoment / (count - 1)) : 0;

        return new double[] { min, max, mean, stdDev };
    }
}