/*
 * MIT License
 *
 * Copyright (c) 2025 Navaneeth Sen <navaneethsen@gmail.com>
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
package org.cache.common;

/**
 * Generic cache interface that supports basic cache operations.
 *
 * @param <K> The type of keys maintained by this cache
 * @param <V> The type of mapped values
 */
public interface ICache<K, V> {
    
    /**
     * Retrieves a value from the cache.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the key, or null if not present
     */
    V get(K key);
    
    /**
     * Inserts or updates a key-value pair in the cache.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    void put(K key, V value);
    
    /**
     * Removes an entry from the cache.
     *
     * @param key the key of the entry to remove
     * @return the value that was associated with the key, or null if not present
     */
    V remove(K key);
    
    /**
     * Returns the current size of the cache.
     *
     * @return the number of entries currently in the cache
     */
    int size();
    
    /**
     * Returns the maximum capacity of the cache.
     *
     * @return the maximum number of entries the cache can hold
     */
    int getCapacity();
    
    /**
     * Clears all entries from the cache.
     */
    void clear();
    
    /**
     * Checks if the cache is empty.
     *
     * @return true if the cache contains no entries, false otherwise
     */
    default boolean isEmpty() {
        return size() == 0;
    }
    
    /**
     * Checks if the cache is at capacity.
     *
     * @return true if the cache is at its maximum capacity, false otherwise
     */
    default boolean isFull() {
        return size() >= getCapacity();
    }
    
    /**
     * Checks if the cache contains the specified key.
     *
     * @param key the key to check for
     * @return true if the cache contains the key, false otherwise
     */
    boolean containsKey(K key);
}
