/*
 *
 * Copyright (c) 2025  Pegasystems Inc.
 * All rights reserved.
 *
 * This  software  has  been  provided pursuant  to  a  License
 * Agreement  containing  restrictions on  its  use.   The  software
 * contains  valuable  trade secrets and proprietary information  of
 * Pegasystems Inc and is protected by  federal   copyright law.  It
 * may  not be copied,  modified,  translated or distributed in  any
 * form or medium,  disclosed to third parties or used in any manner
 * not provided for in  said  License Agreement except with  written
 * authorization from Pegasystems Inc.
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