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

package permafrost.tundra.idata;

import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataPortable;
import com.wm.util.coder.IDataCodable;
import com.wm.util.coder.ValuesCodable;

/**
 * Compares two IData objects using all the keys and values in each document.
 */
public class IDataDefaultComparator implements IDataComparator {
    /**
     * Compares two IData documents.
     *
     * @param document1  The first IData document to be compared.
     * @param document2 The second IData document to be compared.
     * @return               A value less than zero if the first document
     *                       comes before the second document, a value of
     *                       zero if they are equal, or a value of greater
     *                       than zero if the first document comes after the
     *                       second document according to the comparison
     *                       of all the keys and values in each document.
     */
    public int compare(IData document1, IData document2) {
        int result = 0;

        if (document1 == null || document2 == null) {
            if (document1 != null) {
                result = 1;
            } else if (document2 != null){
                result = -1;
            }
        } else {
            // compare all keys and values in order
            IDataCursor cursor1 = document1.getCursor();
            IDataCursor cursor2 = document2.getCursor();

            boolean next1 = cursor1.next(), next2 = cursor2.next();

            while(next1 && next2) {
                String key1 = cursor1.getKey();
                String key2 = cursor2.getKey();

                result = key1.compareTo(key2);

                if (result == 0) {
                    Object value1 = cursor1.getValue();
                    Object value2 = cursor2.getValue();

                    if (value1 == null || value2 == null) {
                        if (value1 != null) {
                            result = 1;
                        } else if (value2 != null) {
                            result = -1;
                        }
                    } else if ((value1 instanceof IData || value1 instanceof IDataCodable || value1 instanceof IDataPortable || value1 instanceof ValuesCodable) &&
                               (value2 instanceof IData || value2 instanceof IDataCodable || value2 instanceof IDataPortable || value2 instanceof ValuesCodable)) {
                        IData id1;

                        if (value1 instanceof IData) {
                            id1 = (IData)value1;
                        } else if (value1 instanceof IDataCodable) {
                            id1 = ((IDataCodable)value1).getIData();
                        } else if (value1 instanceof IDataPortable) {
                            id1 = ((IDataPortable)value1).getAsData();
                        } else {
                            id1 = ((ValuesCodable)value1).getValues();
                        }

                        IData id2;

                        if (value2 instanceof IData) {
                            id2 = (IData)value2;
                        } else if (value2 instanceof IDataCodable) {
                            id2 = ((IDataCodable)value2).getIData();
                        } else if (value2 instanceof IDataPortable) {
                            id2 = ((IDataPortable)value2).getAsData();
                        } else {
                            id2 = ((ValuesCodable)value2).getValues();
                        }

                        result = compare(id1, id2);
                        //TODO: IData[], Table, etc.
                        //TODO: Object[], Object[][]

                    } else if (value1 instanceof Comparable && value2 instanceof Comparable && value1.getClass().isAssignableFrom(value2.getClass())) {
                        result = ((Comparable)value1).compareTo((Comparable)value2);
                    } else if (value1 instanceof Comparable && value2 instanceof Comparable && value2.getClass().isAssignableFrom(value1.getClass())) {
                        int comparison = ((Comparable)value2).compareTo((Comparable)value1);
                        result = comparison < 0 ? 1 : comparison > 0 ? -1 : 0;
                    } else if (value1 != value2) {
                        // last ditch effort to compare two uncomparable objects using hash codes?
                        result = Integer.valueOf(value1.hashCode()).compareTo(value2.hashCode());
                        if (result == 0) result = 1; // make an arbitrary choice
                    }
                }

                if (result != 0) {
                    break;
                }

                next1 = cursor1.next();
                next2 = cursor2.next();
            }

            if (next1 && !next2) {
                result = 1;
            } else if (!next1 && next2) {
                result = -1;
            }
        }

        return result;
    }
}
