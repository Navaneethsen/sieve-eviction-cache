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

import java.util.concurrent.atomic.LongAdder;

/**
 * Abstract base class for cache implementations that provides common functionality.
 *
 * @param <K> The type of keys maintained by this cache
 * @param <V> The type of mapped values
 */
public abstract class AbstractCache<K, V> implements IStatisticalCache<K, V> {
    
    protected final int capacity;
    
    // Statistics (concurrency-friendly)
    protected final LongAdder hits = new LongAdder();
    protected final LongAdder misses = new LongAdder();
    protected final LongAdder evictions = new LongAdder();
    
    protected AbstractCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
    }
    
    @Override
    public int getCapacity() {
        return capacity;
    }
    
    @Override
    public CacheStats getStats() {
        return new CacheStats(hits.sum(), misses.sum(), evictions.sum());
    }
    
    @Override
    public void clearStats() {
        hits.reset();
        misses.reset();
        evictions.reset();
    }
    
    /**
     * Records a cache hit.
     */
    @Override
    public void recordHit() {
        hits.increment();
    }
    
    /**
     * Records a cache miss.
     */
    @Override
    public void recordMiss() {
        misses.increment();
    }
    
    /**
     * Records an eviction.
     */
    @Override
    public void recordEviction() {
        evictions.increment();
    }
    
    /**
     * Validates that key and value are not null.
     *
     * @param key the key to validate
     * @param value the value to validate
     * @throws NullPointerException if key or value is null
     */
    protected void validateKeyValue(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("Key and value cannot be null");
        }
    }
}
