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
 * Extension of Cache interface that provides statistical information.
 *
 * @param <K> The type of keys maintained by this cache
 * @param <V> The type of mapped values
 */
public interface IStatisticalCache<K, V> extends ICache<K, V> {
    
    /**
     * Returns cache statistics.
     *
     * @return immutable cache statistics
     */
    CacheStats getStats();
    
    /**
     * Clears all statistics counters.
     */
    void clearStats();
    
    /**
     * Records a cache hit.
     */
    void recordHit();
    
    /**
     * Records a cache miss.
     */
    void recordMiss();
    
    /**
     * Records an eviction.
     */
    void recordEviction();
    
    /**
     * Returns the current hit rate as a percentage.
     *
     * @return hit rate percentage (0.0 to 100.0)
     */
    default double getHitRatePercent() {
        return getStats().getHitRatePercent();
    }
    
    /**
     * Returns the current miss rate as a percentage.
     *
     * @return miss rate percentage (0.0 to 100.0)
     */
    default double getMissRatePercent() {
        return getStats().getMissRatePercent();
    }
}
