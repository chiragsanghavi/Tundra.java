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

package permafrost.tundra.collection;

import com.wm.data.IData;
import com.wm.data.IDataCursor;
import permafrost.tundra.data.MapIData;
import java.util.Map;
import java.util.TreeMap;

/**
 * A collection of convenience methods for working with java.util.Map objects.
 */
public class MapHelper {
    /**
     * Returns the value to which the specified key is mapped, or null if this map contains no mapping for the key.
     *
     * @param map   The map to be operated on.
     * @param key   The key whose associated value is to be returned.
     * @param <K>   The class of keys in this map.
     * @param <V>   The class of values in the map.
     * @return      The value to which the specified key is mapped, or null if this map contains no mapping for the key.
     */
    public static <K, V> V get(Map<K, V> map, K key) {
        V value = null;
        if (map != null) {
            value = map.get(key);
        }
        return value;
    }

    /**
     * Associates the specified value with the specified key in this map. If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     *
     * @param map   The map to be operated on.
     * @param key   Key with which the specified value is to be associated.
     * @param value Value to be associated with the specified key.
     * @param <K>   The class of keys in this map.
     * @param <V>   The class of values in the map.
     */
    public static <K, V> void put(Map<K, V> map, K key, V value) {
        if (map != null) {
            map.put(key, value);
        }
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @param map   The map to be operated on.
     * @param <K>   The class of keys in this map.
     * @param <V>   The class of values in the map.
     * @return      The number of key-value mappings in this map.
     */
    public static <K, V> int length(Map<K, V> map) {
        if (map == null) return 0;
        return map.size();
    }

    /**
     * Removes all of the mappings from this map (optional operation). The map will be empty after this call returns.
     *
     * @param map   The map to be operated on.
     * @param <K>   The class of keys in this map.
     * @param <V>   The class of values in the map.
     */
    public static <K, V> void clear(Map<K, V> map) {
        if (map != null) map.clear();
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param map   The map to be operated on.
     * @param key   The key whose mapping is to be removed from the map.
     * @param <K>   The class of keys in this map.
     * @param <V>   The class of values in the map.
     * @return      The previous value associated with key, or null if there was no mapping for key.
     */
    public static <K, V> V remove(Map<K, V> map, K key) {
        if (map == null) return null;
        return map.remove(key);
    }

    /**
     * Returns a newly created Map object.
     *
     * @param <K>                   The class of Map keys.
     * @param <V>                   The class of Map values.
     * @return                      A newly created Map object.
     */
    public static <K, V> Map<K, V> create() {
        return new TreeMap<K, V>();
    }

    /**
     * Converts the given IData document to a Map.
     *
     * @param document              The IData document to be converted.
     * @param <K>                   The class of Map keys.
     * @param <V>                   The class of Map values.
     * @return                      A newly created Map which contains the top-level key value elements from the
     *                              given document.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> mapify(IData document) {
        if (document == null) return null;

        Map<K, V> map = create();

        IDataCursor cursor = document.getCursor();
        while(cursor.next()) {
            map.put((K)cursor.getKey(), (V)cursor.getValue());
        }

        // wrap the map in an IData compatible wrapper for developer convenience
        return new MapIData<K, V>(map);
    }
}
