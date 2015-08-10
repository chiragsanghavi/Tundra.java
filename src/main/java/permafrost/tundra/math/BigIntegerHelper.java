/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Lachlan Dowding
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package permafrost.tundra.math;

import java.math.BigInteger;

/**
 * A collection of convenience methods for working with integers.
 */
public class BigIntegerHelper {
    /**
     * The default radix used by methods in this class.
     */
    public static int DEFAULT_RADIX = 10;

    /**
     * Disallow instantiation of this class.
     */
    private BigIntegerHelper() {}

    /**
     * Returns a java.math.BigInteger object by parsing the given an integer string.
     * @param string A string to be parsed.
     * @return       A java.math.BigInteger representation of the given string.
     */
    public static BigInteger parse(String string) {
        return parse(string, DEFAULT_RADIX);
    }

    /**
     * Returns a java.math.BigInteger object by parsing the given an integer string,
     * using the given radix.
     *
     * @param string A string to be parsed.
     * @param radix  The radix to use when interpreting the given string.
     * @return       A java.math.BigInteger representation of the given string.
     */
    public static BigInteger parse(String string, int radix) {
        if (string == null) return null;
        return new BigInteger(string, radix);
    }

    /**
     * Returns java.math.BigInteger representations of the given String[].
     * @param strings A list of strings to parse.
     * @return        A list of java.math.BigDecimal representations of the given strings.
     */
    public static BigInteger[] parse(String[] strings) {
        return parse(strings, DEFAULT_RADIX);
    }

    /**
     * Returns java.math.BigInteger representations of the given String[].
     * @param strings A list of strings to parse.
     * @param radix   The radix to use when interpreting the given strings.
     * @return        A list of java.math.BigDecimal representations of the given strings.
     */
    public static BigInteger[] parse(String[] strings, int radix) {
        if (strings == null) return null;

        BigInteger[] integers = new BigInteger[strings.length];

        for (int i = 0; i < strings.length; i++) {
            integers[i] = parse(strings[i], radix);
        }

        return integers;
    }
}