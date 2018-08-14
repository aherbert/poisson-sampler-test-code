package org.apache.commons.rng.sampling;

import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class HexStringSamplerTest {

    @Test
    public void testConstructor() {
        final UniformRandomProvider rng = null;
        final int length = 1;
        final HexStringSampler s = new HexStringSampler(rng, length);
        Assert.assertNotNull(s);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrows() {
        final UniformRandomProvider rng = null;
        final int length = 0;
        @SuppressWarnings("unused")
        final HexStringSampler s = new HexStringSampler(rng, length);
    }

    @Test
    public void testSamples() {
        // Get the valid range for lower case hex digits
        final int lower1 = '0';
        final int upper1 = '9';
        final int lower2 = 'a';
        final int upper2 = 'f';

        final UniformRandomProvider rng = RandomSource.create(RandomSource.MWC_256);
        final int[] lengths = new int[] { 1, 5, 10 };
        for (final int length : lengths) {
            final HexStringSampler s = new HexStringSampler(rng, length);
            for (int i = 0; i < 10; i++) {
                final String hex = s.sample();
                Assert.assertNotNull(hex);
                // System.out.println(hex);
                Assert.assertEquals(length, hex.length());
                for (int j = 0; j < length; j++) {
                    final int c = hex.charAt(j);
                    final boolean isHex = (lower1 <= c && c <= upper1) || (lower2 <= c && c <= upper2);
                    Assert.assertTrue(isHex);
                }
            }
        }
    }

    @Test
    public void testSamplesVersesCommonsMath() {

        final RestorableUniformRandomProvider rng1 = RandomSource.create(RandomSource.MWC_256);
        final RestorableUniformRandomProvider rng2 = RandomSource.create(RandomSource.MWC_256);
        rng2.restoreState(rng1.saveState());

        final int[] lengths = new int[] { 1, 5, 10 };
        for (final int length : lengths) {
            final HexStringSampler s = new HexStringSampler(rng1, length);
            for (int i = 0; i < 10; i++) {
                final String hex1 = s.sample();
                final String hex2 = nextHexString(rng2, length);
                Assert.assertEquals(hex2, hex1);
            }
        }
    }

    /**
     * Adapted from RandomDataGenerator to match the implementation of the
     * HexStringSampler. Original code is left commented out.
     *
     * @param ran a random number generator
     * @param len the desired string length.
     * @return the random string.
     * @throws NotStrictlyPositiveException if {@code len <= 0}.
     */
    public String nextHexString(UniformRandomProvider ran, int len) {

        // Initialize output buffer
        StringBuilder outBuffer = new StringBuilder();

        // Get int(len/2)+1 random bytes
        // byte[] randomBytes = new byte[(len/2) + 1]; // ORIGINAL
        byte[] randomBytes = new byte[(len + 1) / 2];
        ran.nextBytes(randomBytes);

        // Convert each byte to 2 hex digits
        for (int i = 0; i < randomBytes.length; i++) {

            /*
             * Add 128 to byte value to make interval 0-255 before doing hex conversion.
             * This guarantees <= 2 hex digits from toHexString() toHexString would
             * otherwise add 2^32 to negative arguments.
             */
            // ORIGINAL
            // Integer c = Integer.valueOf(randomBytes[i]);
            // String hex = Integer.toHexString(c.intValue() + 128);

            String hex = Integer.toHexString(randomBytes[i] & 0xff);

            // Make sure we add 2 hex digits for each byte
            if (hex.length() == 1) {
                hex = "0" + hex;
            }
            outBuffer.append(hex);
        }
        return outBuffer.toString().substring(0, len);
    }

    @Test
    public void testSamplesAreUniform() {
        // Get the valid range for lower case hex digits
        final int lower1 = '0';
        final int upper1 = '9';
        final int lower2 = 'a';
        final int offset2 = lower2 - 10;
        final int[] h = new int[16];

        final UniformRandomProvider rng = RandomSource.create(RandomSource.MWC_256);
        int length = 1000;
        int repeats = 100;
        final HexStringSampler s = new HexStringSampler(rng, length);
        for (int i = 0; i < repeats; i++) {
            final String hex = s.sample();
            for (int j = 0; j < length; j++) {
                final int c = hex.charAt(j);
                if (lower1 <= c && c <= upper1)
                    h[c - lower1]++;
                else
                    h[c - offset2]++;
            }
        }

        // TODO - Statistical test: Kolmogorov Smirnov
        // https://math.stackexchange.com/questions/2435/is-there-a-simple-test-for-uniform-distributions
        double mean = length * repeats / 16.0;
        for (int i = 0; i < h.length; i++) {
            System.out.printf("%2d = %d  (%.2f)\n", i, h[i], h[i] / mean);
        }
    }

    // TODO - Test the static method returns the same strings
}
