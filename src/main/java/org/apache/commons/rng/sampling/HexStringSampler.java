package org.apache.commons.rng.sampling;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Class for generating random hex strings.
 */
public class HexStringSampler {
    /**
     * Used to build output as Hex
     */
    private static final char[] DIGITS_LOWER = { 
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private final UniformRandomProvider rng;
    private final byte[] bytes;
    private final int length;

    /**
     * Creates a generator of hex strings of the given length.
     *
     * @param rng    Generator of uniformly distributed random numbers.
     * @param length The length.
     */
    public HexStringSampler(UniformRandomProvider rng, int length) {
        this.rng = rng;
        this.bytes = createByteBuffer(length);
        this.length = length;
    }

    /**
     * Creates the byte buffer used for the random sample.
     *
     * @param length The length.
     * @return The byte buffer.
     */
    private static byte[] createByteBuffer(int length) {
        if (length <= 0)
            throw new IllegalArgumentException(length + " <= 0");
        // The random sample of bytes creates 2 hex 
        // characters per byte.
        // Get enough bytes to cover the length, 
        // i.e. round odd lengths up and divide by 2.
        return new byte[(length + 1) / 2];
    }

    /**
     * @return A random hex string.
     * 
     * @see #nextHexString(UniformRandomProvider, int)
     */
    public String sample() {
        return nextHexString(rng, bytes, length);
    }

    /**
     * Generate a random hex string of the given length.
     *
     * @param rng    Generator of uniformly distributed random numbers.
     * @param length The length.
     * @return A random hex string.
     */
    public static String nextHexString(UniformRandomProvider rng, int length) {
        return nextHexString(rng, createByteBuffer(length), length);
    }

    /**
     * Generate a random hex string of the given length.
     * <p>
     * No checks are made that the byte buffer is the appropriate size.
     *
     * @param rng    Generator of uniformly distributed random numbers.
     * @param bytes  The byte buffer.
     * @param length The length.
     * @return A random hex string.
     */
    private static String nextHexString(UniformRandomProvider rng, byte[] bytes, int length) {
        rng.nextBytes(bytes);
        // Use the upper and lower 4 bits of each byte as an
        // index in the range 0-15 for each hex characters.
        final char[] out = new char[length];
        // Run the loop without checking index j by 
        // producing hex characters pairs up to the size 
        // below the desired length.
        final int loopLimit = length / 2;
        int i = 0, j = 0;
        while (i < loopLimit) {
            out[j++] = DIGITS_LOWER[(0xF0 & bytes[i]) >>> 4];
            out[j++] = DIGITS_LOWER[0x0F & bytes[i]];
            i++;
        }
        // The final hex character if length is odd
        if (j < length) {
            out[j++] = DIGITS_LOWER[(0xF0 & bytes[i]) >>> 4];
        }
        return new String(out);
    }
}
