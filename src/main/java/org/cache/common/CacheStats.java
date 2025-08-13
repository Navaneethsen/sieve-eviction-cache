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
 * Immutable cache statistics holder.
 */
public final class CacheStats{
    private final long hits;
    private final long misses;
    private final long evictions;
    private final long totalRequests;
    private final double hitRate;
    private final double missRate;
    
    public CacheStats(long hits, long misses, long evictions) {
        this.hits = Math.max(0, hits);
        this.misses = Math.max(0, misses);
        this.evictions = Math.max(0, evictions);
        this.totalRequests = this.hits + this.misses;
        this.hitRate = totalRequests == 0 ? 0.0 : (double) hits / totalRequests;
        this.missRate = totalRequests == 0 ? 0.0 : (double) misses / totalRequests;
    }
    
    public long getHits() {
        return hits;
    }
    
    public long getMisses() {
        return misses;
    }
    
    public long getEvictions() {
        return evictions;
    }
    
    public long getTotalRequests() {
        return totalRequests;
    }
    
    public double getHitRate() {
        return hitRate;
    }
    
    public double getMissRate() {
        return missRate;
    }
    
    public double getHitRatePercent() {
        return hitRate * 100.0;
    }
    
    public double getMissRatePercent() {
        return missRate * 100.0;
    }
    
    @Override
    public String toString() {
        return String.format(
                "CacheStats{hits=%d, misses=%d, evictions=%d, hitRate=%.2f%%, requests=%d}",
                hits, misses, evictions, getHitRatePercent(), totalRequests
        );
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CacheStats that = (CacheStats) obj;
        return hits == that.hits && misses == that.misses && evictions == that.evictions;
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(hits, misses, evictions);
    }
}