/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Lachlan Dowding
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

package permafrost.tundra.io.filter;

import permafrost.tundra.collection.ListHelper;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A collection of convenience methods for FilenameFilter objects.
 */
public class FilenameFilterHelper {
    /**
     * Disallow instantiation of this class.
     */
    private FilenameFilterHelper() {}

    /**
     * Creates a list of FilenameFilters given a list of patterns.
     *
     * @param type      The type of pattern provided.
     * @param patterns  A list of patterns to be used as FilenameFilter objects.
     * @return          A list of FilenameFilter objects using the given patterns.
     */
    public static FilenameFilter[] create(FilenameFilterType type, String... patterns) {
        Collection<FilenameFilter> filters = create(type, ListHelper.of(patterns));
        return filters == null ? null : filters.toArray(new FilenameFilter[0]);
    }

    /**
     * Creates a list of FilenameFilters given a list of patterns.
     *
     * @param type      The type of pattern provided.
     * @param patterns  A list of patterns to be used as FilenameFilter objects.
     * @return          A list of FilenameFilter objects using the given patterns.
     */
    public static Collection<FilenameFilter> create(FilenameFilterType type, Collection<String> patterns) {
        if (patterns == null) return null;

        List<FilenameFilter> filters = new ArrayList<FilenameFilter>(patterns.size());

        for (String pattern : patterns) {
            if (pattern != null) {
                if (type == FilenameFilterType.REGULAR_EXPRESSION) {
                    filters.add(new RegularExpressionFilenameFilter(pattern));
                } else {
                    filters.add(new WildcardFilenameFilter(pattern));
                }
            }
        }

        return filters;
    }

    /**
     * Combines the given list of filters using a logical AND operation.
     *
     * @param filters   The list of filters to be combined.
     * @return          A filter that combines all the given filters with a logical AND operation.
     */
    public static FilenameFilter and(FilenameFilter ...filters) {
        AndFilenameFilter combinedFilter = null;

        if (filters != null) {
            for (FilenameFilter filter : filters) {
                if (filter != null) {
                    if (combinedFilter == null) {
                        combinedFilter = new AndFilenameFilter();
                    }
                    combinedFilter.add(filter);
                }
            }
        }

        return combinedFilter;
    }

    /**
     * Combines the given list of filters using a logical OR operation.
     *
     * @param filters   The list of filters to be combined.
     * @return          A filter that combines all the given filters with a logical OR operation.
     */
    public static FilenameFilter or(FilenameFilter ...filters) {
        OrFilenameFilter combinedFilter = null;

        if (filters != null) {
            for (FilenameFilter filter : filters) {
                if (filter != null) {
                    if (combinedFilter == null) {
                        combinedFilter = new OrFilenameFilter();
                    }
                    combinedFilter.add(filter);
                }
            }
        }

        return combinedFilter;
    }
}
