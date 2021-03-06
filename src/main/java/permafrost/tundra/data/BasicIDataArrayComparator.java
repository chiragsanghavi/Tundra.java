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

package permafrost.tundra.data;

import com.wm.data.IData;

/**
 * Compares two IData[] objects using by comparing each item's keys and values.
 */
public class BasicIDataArrayComparator implements IDataArrayComparator {
    /**
     * Whether key order is treated as significant, such that two documents with the same keys and values specified
     * in differing orders are not considered equivalent.
     */
    protected boolean isKeyOrderSignificant = true;

    /**
     * Construct a new BasicIDataComparator.
     */
    public BasicIDataArrayComparator() {}

    /**
     * Construct a new BasicIDataComparator.
     *
     * @param isKeyOrderSignificant Whether the ordering of keys is considered significant when comparing documents.
     */
    public BasicIDataArrayComparator(boolean isKeyOrderSignificant) {
        this.isKeyOrderSignificant = isKeyOrderSignificant;
    }

    /**
     * Compares two IData[] objects.
     *
     * @param array1 The first IData[] to be compared.
     * @param array2 The second IData[] to be compared.
     * @return A value less than zero if the first array comes before the second array, a value of zero if they are
     * equal, or a value of greater than zero if the first array comes after the second array according to the
     * comparison of each item.
     */
    public int compare(IData[] array1, IData[] array2) {
        return IDataHelper.compare(array1, array2, isKeyOrderSignificant);
    }
}
