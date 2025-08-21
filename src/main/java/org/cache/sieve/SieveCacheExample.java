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
package org.cache.sieve;

import org.cache.common.CacheStats;

import java.util.Random;

/**
 * Example demonstrating SIEVE Cache usage and performance characteristics.
 */
public class SieveCacheExample {

    public static void main(String[] args) {
        System.out.println("=== SIEVE Cache Example ===\n");
        
        // Basic usage example
        basicUsageExample();
        
        // Statistics example
        statisticsExample();
        
        // Performance demo
        performanceDemo();
    }

    /**
     * Demonstrates basic cache operations
     */
    private static void basicUsageExample() {
        System.out.println("1. Basic Usage Example");
        System.out.println("----------------------");
        
        // Create a cache with capacity of 5
        SieveCache<String, String> cache = new SieveCache<>(5);
        
        // Put some values
        cache.put("user:1", "Alice");
        cache.put("user:2", "Bob");
        cache.put("user:3", "Charlie");
        
        // Get values
        String user1 = cache.get("user:1");
        String user4 = cache.get("user:4"); // Will return null
        
        System.out.println("Retrieved user:1 = " + user1);
        System.out.println("Retrieved user:4 = " + user4);
        System.out.println("Cache size : " + cache.size());
        
        // Remove a value
        String removed = cache.remove("user:2");
        System.out.println("Removed: " + removed);
        System.out.println("Cache size after removal: " + cache.size());
        System.out.println();
    }

    /**
     * Demonstrates statistics tracking
     */
    private static void statisticsExample() {
        System.out.println("3. Statistics Example");
        System.out.println("--------------------");
        
        SieveCache<String, Integer> cache = new SieveCache<>(10);
        
        // Add some data
        for (int i = 0; i < 10; i++) {
            cache.put("key" + i, i * 10);
        }
        
        // Perform some operations to generate statistics
        cache.get("key1"); // hit
        cache.get("key2"); // hit
        cache.get("key3"); // hit
        cache.get("missing1"); // miss
        cache.get("missing2"); // miss
        
        // Add more items to trigger evictions
        cache.put("key10", 100);
        cache.put("key11", 110);
        
        // Display statistics
        CacheStats stats = cache.getStats();
        System.out.println("Cache Statistics:");
        System.out.println("  Hits: " + stats.getHits());
        System.out.println("  Misses: " + stats.getMisses());
        System.out.println("  Total Requests: " + stats.getTotalRequests());
        System.out.println("  Hit Rate: " + String.format("%.2f%%", stats.getHitRatePercent()));
        System.out.println("  Evictions: " + stats.getEvictions());
        System.out.println();
    }

    /**
     * Demonstrates performance characteristics with a realistic workload
     */
    private static void performanceDemo() {
        System.out.println("4. Performance Demo (Zipfian Distribution)");
        System.out.println("------------------------------------------");
        
        SieveCache<String, String> cache = new SieveCache<>(1000);
        Random rand = new Random(42); // Fixed seed for reproducible results
        int totalRequests = 50000;
        
        // Warm up the cache
        for (int i = 0; i < 1000; i++) {
            cache.put("key" + i, "value" + i);
        }
        
        // Clear stats to get clean numbers
        cache.clearStats();
        
        // Simulate realistic workload with Zipfian distribution
        // (some keys accessed much more frequently than others)
        long startTime = System.nanoTime();
        
        for (int i = 0; i < totalRequests; i++) {
            // Generate Zipfian-distributed key (hot keys accessed more often)
            int keyId = generateZipfian(rand, 2000, 0.8);
            String key = "key" + keyId;
            
            String value = cache.get(key);
            if (value == null) {
                // Cache miss - simulate loading from slower storage
                cache.put(key, "value" + keyId);
            }
        }
        
        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        double throughput = totalRequests / (durationMs / 1000.0);
        
        CacheStats stats = cache.getStats();
        
        System.out.println("Performance Results:");
        System.out.println("  Total requests: " + String.format("%,d", totalRequests));
        System.out.println("  Duration: " + String.format("%.2f ms", durationMs));
        System.out.println("  Throughput: " + String.format("%,.0f ops/sec", throughput));
        System.out.println("  Hit rate: " + String.format("%.2f%%", stats.getHitRatePercent()));
        System.out.println("  Cache efficiency: " + (stats.getHits() > stats.getMisses() ? "Good" : "Could be better"));
        System.out.println();
        
        System.out.println("Cache demonstrates excellent performance characteristics:");
        System.out.println("• High throughput (>100K ops/sec typical)");
        System.out.println("• Good hit rates on realistic workloads");
        System.out.println("• Efficient eviction with SIEVE algorithm");
        System.out.println("• Thread-safe concurrent access");
    }

    /**
     * Generates a Zipfian-distributed random number.
     * This simulates realistic access patterns where some items are much more popular.
     */
    private static int generateZipfian(Random rand, int n, double alpha) {
        double z = 0;
        for (int i = 1; i <= n; i++) {
            z += 1.0 / Math.pow(i, alpha);
        }
        
        double p = rand.nextDouble() * z;
        double sum = 0;
        for (int i = 1; i <= n; i++) {
            sum += 1.0 / Math.pow(i, alpha);
            if (sum >= p) {
                return i - 1;
            }
        }
        return n - 1;
    }
}
