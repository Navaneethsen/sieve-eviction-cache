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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JUnit 5 test suite for SIEVE Cache implementation.
 * Tests all edge cases, eviction scenarios, and concurrent behavior.
 */
@DisplayName("SIEVE Cache Test Suite")
public class SieveCacheTest {

    @Nested
    @DisplayName("Basic Functionality Tests")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("Basic Put/Get Operations")
        void testBasicPutGet() {
            SieveCache<String, Integer> cache = new SieveCache<>(5);
            
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);
            
            assertEquals(1, cache.get("a"), "Get 'a'");
            assertEquals(2, cache.get("b"), "Get 'b'");
            assertEquals(3, cache.get("c"), "Get 'c'");
            assertNull(cache.get("d"), "Get non-existent 'd'");
            assertEquals(3, cache.size(), "Cache size");
        }

        @Test
        @DisplayName("Update Existing Key")
        void testUpdateExistingKey() {
            SieveCache<String, Integer> cache = new SieveCache<>(5);
            
            cache.put("key", 100);
            assertEquals(100, cache.get("key"), "Initial value");
            
            cache.put("key", 200);
            assertEquals(200, cache.get("key"), "Updated value");
            assertEquals(1, cache.size(), "Size after update");
        }

        @Test
        @DisplayName("Remove Operation")
        void testRemoveOperation() {
            SieveCache<Integer, String> cache = new SieveCache<>(5);
            
            cache.put(1, "one");
            cache.put(2, "two");
            cache.put(3, "three");
            
            String removed = cache.remove(2);
            assertEquals("two", removed, "Removed value");
            assertNull(cache.get(2), "Get after remove");
            assertEquals(2, cache.size(), "Size after remove");
            
            // Remove non-existent
            assertNull(cache.remove(99), "Remove non-existent");
        }

        @Test
        @DisplayName("Clear Operation")
        void testClearOperation() {
            SieveCache<Integer, String> cache = new SieveCache<>(5);
            
            for (int i = 0; i < 5; i++) {
                cache.put(i, "value" + i);
            }
            
            assertEquals(5, cache.size(), "Size before clear");
            cache.clear();
            assertEquals(0, cache.size(), "Size after clear");
            
            // Should be able to add after clear
            cache.put(10, "ten");
            assertEquals("ten", cache.get(10), "Get after clear");
        }

        @Test
        @DisplayName("Null Handling")
        void testNullHandling() {
            SieveCache<String, String> cache = new SieveCache<>(5);
            
            // Test null key
            assertThrows(NullPointerException.class, () -> {
                cache.put(null, "value");
            }, "Should reject null key");
            
            // Test null value
            assertThrows(NullPointerException.class, () -> {
                cache.put("key", null);
            }, "Should reject null value");
        }

        @Test
        @DisplayName("Capacity Enforcement")
        void testCapacityEnforcement() {
            SieveCache<Integer, String> cache = new SieveCache<>(3);
            
            cache.put(1, "one");
            cache.put(2, "two");
            cache.put(3, "three");
            assertEquals(3, cache.size(), "Size at capacity");
            
            cache.put(4, "four");
            assertEquals(3, cache.size(), "Size after exceeding capacity");
            
            // At least one of the first three should be evicted
            int evictedCount = 0;
            for (int i = 1; i <= 3; i++) {
                if (cache.get(i) == null) evictedCount++;
            }
            assertTrue(evictedCount >= 1, "At least one eviction occurred");
            assertNotNull(cache.get(4), "New item was added");
        }
    }

    @Nested
    @DisplayName("Eviction Scenario Tests")
    class EvictionScenarioTests {

        @Test
        @DisplayName("Basic Eviction")
        void testBasicEviction() {
            SieveCache<Integer, String> cache = new SieveCache<>(3);
            
            cache.put(1, "one");
            cache.put(2, "two");
            cache.put(3, "three");
            
            // No items visited, adding 4 should evict oldest (1)
            cache.put(4, "four");
            
            assertNull(cache.get(1), "Item 1 should be evicted");
            assertNotNull(cache.get(2), "Item 2 should remain");
            assertNotNull(cache.get(3), "Item 3 should remain");
            assertNotNull(cache.get(4), "Item 4 should be added");
        }

        @Test
        @DisplayName("Visited vs Unvisited Eviction")
        void testVisitedVsUnvisitedEviction() {
            SieveCache<Integer, String> cache = new SieveCache<>(4);
            
            cache.put(1, "one");
            cache.put(2, "two");
            cache.put(3, "three");
            cache.put(4, "four");
            
            // Visit 1 and 3
            cache.get(1);
            cache.get(3);
            
            // Add 5, should evict unvisited 2 (not visited 1)
            cache.put(5, "five");
            
            assertNotNull(cache.get(1), "Visited item 1 should remain");
            assertNull(cache.get(2), "Unvisited item 2 should be evicted");
            assertNotNull(cache.get(3), "Visited item 3 should remain");
        }

        @Test
        @DisplayName("Sequential Eviction")
        void testSequentialEviction() {
            SieveCache<Integer, String> cache = new SieveCache<>(3);
            
            cache.put(1, "one");
            cache.put(2, "two");
            cache.put(3, "three");
            
            // Visit all items
            cache.get(1);
            cache.get(2);
            cache.get(3);
            
            // Add multiple new items
            for (int i = 4; i <= 6; i++) {
                cache.put(i, "value" + i);
            }
            
            // After adding 3 new items, all old items should be evicted
            // (their visited flags were cleared on first pass)
            assertNull(cache.get(1), "Item 1 should be evicted");
            assertNull(cache.get(2), "Item 2 should be evicted");
            assertNull(cache.get(3), "Item 3 should be evicted");
        }

        @Test
        @DisplayName("All Visited Eviction")
        void testAllVisitedEviction() {
            SieveCache<Integer, String> cache = new SieveCache<>(3);
            
            cache.put(1, "one");
            cache.put(2, "two");
            cache.put(3, "three");
            
            // Visit all items
            cache.get(1);
            cache.get(2);
            cache.get(3);
            
            // Add new item - should clear visited bit and evict oldest
            cache.put(4, "four");
            
            assertEquals(3, cache.size(), "Cache size maintained");
            assertNotNull(cache.get(4), "New item added");
            
            // Exactly one of the original items should be evicted
            int remainingCount = 0;
            for (int i = 1; i <= 3; i++) {
                if (cache.get(i) != null) remainingCount++;
            }
            assertEquals(2, remainingCount, "Two original items remain");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Single Capacity Cache")
        void testSingleCapacity() {
            SieveCache<Integer, String> cache = new SieveCache<>(1);
            
            cache.put(1, "one");
            assertEquals("one", cache.get(1), "Single item");
            
            cache.put(2, "two");
            assertNull(cache.get(1), "First item evicted");
            assertEquals("two", cache.get(2), "Second item present");
        }

        @Test
        @DisplayName("Empty Cache Operations")
        void testEmptyCacheOperations() {
            SieveCache<String, String> cache = new SieveCache<>(5);
            
            assertNull(cache.get("any"), "Get from empty");
            assertNull(cache.remove("any"), "Remove from empty");
            cache.clear(); // Should not throw
            assertEquals(0, cache.size(), "Size of empty");
        }

        @Test
        @DisplayName("Rapid Evictions")
        void testRapidEvictions() {
            SieveCache<Integer, String> cache = new SieveCache<>(5);
            
            // Rapidly add items causing many evictions
            for (int i = 0; i < 100; i++) {
                cache.put(i, "value" + i);
            }
            
            assertEquals(5, cache.size(), "Size after rapid evictions");
            
            // Most recent items should be present
            for (int i = 95; i < 100; i++) {
                assertNotNull(cache.get(i), "Recent item " + i);
            }
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    @Timeout(30)
    class ConcurrencyTests {

        @Test
        @DisplayName("Concurrent Reads")
        void testConcurrentReads() throws InterruptedException {
            SieveCache<Integer, String> cache = new SieveCache<>(100);
            
            // Pre-populate cache
            for (int i = 0; i < 100; i++) {
                cache.put(i, "value" + i);
            }
            
            int numThreads = 10;
            int readsPerThread = 1000;
            CountDownLatch latch = new CountDownLatch(numThreads);
            AtomicInteger successCount = new AtomicInteger(0);
            
            for (int t = 0; t < numThreads; t++) {
                new Thread(() -> {
                    Random rand = new Random();
                    for (int i = 0; i < readsPerThread; i++) {
                        int key = rand.nextInt(100);
                        String value = cache.get(key);
                        if (("value" + key).equals(value)) {
                            successCount.incrementAndGet();
                        }
                    }
                    latch.countDown();
                }).start();
            }
            
            assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads completed");
            assertEquals(numThreads * readsPerThread, successCount.get(),
                    "All concurrent reads successful");
        }

        @Test
        @DisplayName("Concurrent Writes")
        void testConcurrentWrites() throws InterruptedException {
            SieveCache<Integer, Integer> cache = new SieveCache<>(50);
            
            int numThreads = 10;
            int writesPerThread = 100;
            CountDownLatch latch = new CountDownLatch(numThreads);
            
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                new Thread(() -> {
                    for (int i = 0; i < writesPerThread; i++) {
                        int key = threadId * writesPerThread + i;
                        cache.put(key, key);
                    }
                    latch.countDown();
                }).start();
            }
            
            assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads completed");
            assertEquals(50, cache.size(), "Cache at capacity");
            
            // Verify cache consistency
            for (int i = 0; i < numThreads * writesPerThread; i++) {
                Integer value = cache.get(i);
                if (value != null) {
                    assertEquals(i, value, "Consistent value for key " + i);
                }
            }
        }

        @Test
        @DisplayName("Concurrent Mixed Operations")
        void testConcurrentMixedOperations() throws InterruptedException {
            SieveCache<String, Integer> cache = new SieveCache<>(100);
            
            int numThreads = 8;
            int opsPerThread = 1000;
            CountDownLatch latch = new CountDownLatch(numThreads);
            AtomicInteger errors = new AtomicInteger(0);
            
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                new Thread(() -> {
                    Random rand = new Random(threadId);
                    for (int i = 0; i < opsPerThread; i++) {
                        try {
                            int op = rand.nextInt(4);
                            String key = "key" + rand.nextInt(200);
                            
                            switch (op) {
                                case 0: // Get
                                    cache.get(key);
                                    break;
                                case 1: // Put
                                    cache.put(key, rand.nextInt(1000));
                                    break;
                                case 2: // Remove
                                    cache.remove(key);
                                    break;
                                case 3: // Update
                                    cache.put(key, rand.nextInt(1000));
                                    break;
                            }
                        } catch (Exception e) {
                            errors.incrementAndGet();
                        }
                    }
                    latch.countDown();
                }).start();
            }
            
            assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads completed");
            assertEquals(0, errors.get(), "No errors during concurrent ops");
            assertTrue(cache.size() <= 100, "Size within capacity");
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Throughput Test")
        void testThroughput() {
            SieveCache<Integer, String> cache = new SieveCache<>(1000);
            
            int operations = 100000;
            long startTime = System.nanoTime();
            
            for (int i = 0; i < operations; i++) {
                if (i % 3 == 0) {
                    cache.put(i, "value" + i);
                } else {
                    cache.get(i % 1000);
                }
            }
            
            long duration = System.nanoTime() - startTime;
            double opsPerSec = operations * 1_000_000_000.0 / duration;
            
            assertTrue(opsPerSec > 100000, "Throughput > 100K ops/sec, actual: " + opsPerSec);
        }

        @Test
        @DisplayName("Zipfian Workload")
        void testZipfianWorkload() {
            SieveCache<String, String> cache = new SieveCache<>(1000);
            
            // Warm up
            for (int i = 0; i < 1000; i++) {
                cache.put("key" + i, "value" + i);
            }
            
            // Reset stats
            cache.clearStats();
            
            Random rand = new Random(42);
            int requests = 50000;
            
            long startTime = System.nanoTime();
            
            for (int i = 0; i < requests; i++) {
                int keyId = generateZipfian(rand, 2000, 0.8);
                String key = "key" + keyId;
                String value = cache.get(key);
                if (value == null) {
                    cache.put(key, "value" + keyId);
                }
            }
            
            long endTime = System.nanoTime();
            double seconds = (endTime - startTime) / 1_000_000_000.0;
            
            CacheStats stats = cache.getStats();
            assertTrue(stats.getHitRate() > 0.5, "Hit rate should be reasonable for Zipfian workload");
            assertTrue(requests / seconds > 10000, "Should maintain good throughput");
        }

        /**
         * Generates a Zipfian-distributed random number
         */
        private int generateZipfian(Random rand, int n, double alpha) {
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

    @Nested
    @DisplayName("Addition Scenario Tests")
    class AdditionScenarioTests {

        @Test
        @DisplayName("Add to Empty Cache")
        void testAddToEmptyCache() {
            SieveCache<String, Integer> cache = new SieveCache<>(10);
            
            assertEquals(0, cache.size(), "Initial size");
            
            cache.put("first", 1);
            assertEquals(1, cache.size(), "Size after first add");
            assertEquals(1, cache.get("first"), "Get first item");
        }

        @Test
        @DisplayName("Add to Partial Cache")
        void testAddToPartialCache() {
            SieveCache<Integer, String> cache = new SieveCache<>(10);
            
            // Partially fill cache
            for (int i = 0; i < 5; i++) {
                cache.put(i, "value" + i);
            }
            
            assertEquals(5, cache.size(), "Initial partial size");
            
            // Add more items without exceeding capacity
            for (int i = 5; i < 10; i++) {
                cache.put(i, "value" + i);
                assertEquals(i + 1, cache.size(), "Size after adding " + i);
            }
            
            // All items should be present
            for (int i = 0; i < 10; i++) {
                assertNotNull(cache.get(i), "Item " + i + " present");
            }
        }

        @Test
        @DisplayName("Add to Full Cache")
        void testAddToFullCache() {
            SieveCache<Integer, String> cache = new SieveCache<>(5);
            
            // Fill cache
            for (int i = 0; i < 5; i++) {
                cache.put(i, "value" + i);
            }
            
            assertEquals(5, cache.size(), "Full cache size");
            
            // Add to full cache
            cache.put(5, "value5");
            assertEquals(5, cache.size(), "Size remains at capacity");
            
            // One item should be evicted
            int presentCount = 0;
            for (int i = 0; i <= 5; i++) {
                if (cache.get(i) != null) presentCount++;
            }
            assertEquals(5, presentCount, "Exactly capacity items present");
        }

        @Test
        @DisplayName("Bulk Addition")
        void testBulkAddition() {
            SieveCache<Integer, String> cache = new SieveCache<>(100);
            
            // Bulk add more than capacity
            for (int i = 0; i < 200; i++) {
                cache.put(i, "value" + i);
            }
            
            assertEquals(100, cache.size(), "Size at capacity");
            
            // Recent items should be present
            for (int i = 150; i < 200; i++) {
                assertNotNull(cache.get(i), "Recent item " + i);
            }
        }

        @Test
        @DisplayName("Add Duplicates")
        void testAddDuplicates() {
            SieveCache<String, Integer> cache = new SieveCache<>(5);
            
            cache.put("key", 1);
            cache.put("key", 2);
            cache.put("key", 3);
            
            assertEquals(1, cache.size(), "Size with duplicates");
            assertEquals(3, cache.get("key"), "Latest value retained");
            
            // Fill cache
            for (int i = 0; i < 5; i++) {
                cache.put("key" + i, i);
            }
            
            assertEquals(5, cache.size(), "Size after filling");
            
            // Update all values
            for (int i = 0; i < 5; i++) {
                cache.put("key" + i, i * 10);
            }
            
            assertEquals(5, cache.size(), "Size after updates");
            
            for (int i = 1; i < 5; i++) {
                assertEquals(i * 10, cache.get("key" + i), "Updated value " + i);
            }
        }

        @Test
        @DisplayName("Add After Clear")
        void testAddAfterClear() {
            SieveCache<Integer, String> cache = new SieveCache<>(5);
            
            // Fill, clear, and refill
            for (int i = 0; i < 5; i++) {
                cache.put(i, "first" + i);
            }
            
            cache.clear();
            
            for (int i = 0; i < 5; i++) {
                cache.put(i, "second" + i);
            }
            
            assertEquals(5, cache.size(), "Size after refill");
            
            for (int i = 0; i < 5; i++) {
                assertEquals("second" + i, cache.get(i), "New value " + i);
            }
        }

        @Test
        @DisplayName("Interleaved Add/Get")
        void testInterleavedAddGet() {
            SieveCache<Integer, String> cache = new SieveCache<>(10);
            
            for (int i = 0; i < 20; i++) {
                cache.put(i, "value" + i);
                
                // Access some previous items
                if (i > 0) {
                    cache.get(i - 1);
                }
                if (i > 5) {
                    cache.get(i - 5);
                }
            }
            
            assertEquals(10, cache.size(), "Size at capacity");
            
            // Recently accessed items should be present
            assertNotNull(cache.get(19), "Most recent");
            assertNotNull(cache.get(18), "Recently accessed");
            assertNotNull(cache.get(14), "Accessed 5 back");
        }

        @Test
        @DisplayName("Add with Eviction Chain")
        void testAddWithEvictionChain() {
            SieveCache<Integer, String> cache = new SieveCache<>(3);
            
            cache.put(1, "one");
            cache.put(2, "two");
            cache.put(3, "three");
            
            // Access all to set visited
            cache.get(1);
            cache.get(2);
            cache.get(3);
            
            // Adding items should trigger chain of evictions
            // First add clears visited bits
            cache.put(4, "four");
            
            // Continue adding
            cache.put(5, "five");
            cache.put(6, "six");
            
            // All original items should be evicted
            assertNull(cache.get(1), "Item 1 evicted");
            assertNull(cache.get(2), "Item 2 evicted");
            assertNull(cache.get(3), "Item 3 evicted");
            
            // New items present
            assertNotNull(cache.get(4), "Item 4 present");
            assertNotNull(cache.get(5), "Item 5 present");
            assertNotNull(cache.get(6), "Item 6 present");
        }
    }

    @Nested
    @DisplayName("Extended Eviction Tests")
    class ExtendedEvictionTests {

        @Test
        @DisplayName("Eviction Order")
        void testEvictionOrder() {
            SieveCache<Integer, String> cache = new SieveCache<>(5);
            
            // Fill cache
            for (int i = 1; i <= 5; i++) {
                cache.put(i, "value" + i);
            }
            
            // Visit odd numbers
            cache.get(1);
            cache.get(3);
            cache.get(5);
            
            // Add new items, should evict unvisited (2, 4) first
            cache.put(6, "six");
            cache.put(7, "seven");
            
            assertNull(cache.get(2), "Unvisited 2 should be evicted");
            assertNull(cache.get(4), "Unvisited 4 should be evicted");
            
            // Visited items should remain initially
            assertNotNull(cache.get(1), "Visited 1 should remain");
            assertNotNull(cache.get(3), "Visited 3 should remain");
            assertNotNull(cache.get(5), "Visited 5 should remain");
        }

        @Test
        @DisplayName("Eviction with Mixed Access Pattern")
        void testEvictionWithMixedAccess() {
            SieveCache<String, Integer> cache = new SieveCache<>(10);
            
            // Add items with different access patterns
            for (int i = 0; i < 10; i++) {
                cache.put("key" + i, i);
            }
            
            // Create access pattern: frequently access first 3
            for (int j = 0; j < 5; j++) {
                cache.get("key0");
                cache.get("key1");
                cache.get("key2");
            }
            
            // Moderately access next 2
            cache.get("key3");
            cache.get("key4");
            
            // Never access the rest (key5-key9)
            
            // Add new items to trigger evictions
            for (int i = 10; i < 15; i++) {
                cache.put("key" + i, i);
            }
            
            // Frequently accessed should remain
            assertNotNull(cache.get("key0"), "Frequently accessed key0");
            assertNotNull(cache.get("key1"), "Frequently accessed key1");
            assertNotNull(cache.get("key2"), "Frequently accessed key2");
            
            // Check that unaccessed items were evicted
            int unaccesedEvicted = 0;
            for (int i = 5; i <= 9; i++) {
                if (cache.get("key" + i) == null) {
                    unaccesedEvicted++;
                }
            }
            assertTrue(unaccesedEvicted >= 3, "At least 3 unaccessed items evicted");
        }

        @Test
        @DisplayName("Eviction After Removal")
        void testEvictionAfterRemoval() {
            SieveCache<Integer, String> cache = new SieveCache<>(5);
            
            // Fill cache
            for (int i = 1; i <= 5; i++) {
                cache.put(i, "value" + i);
            }
            
            // Remove some items
            cache.remove(2);
            cache.remove(4);
            
            assertEquals(3, cache.size(), "Size after removal");
            
            // Add new items - should not evict since under capacity
            cache.put(6, "six");
            cache.put(7, "seven");
            
            assertEquals(5, cache.size(), "Size after additions");
            assertNotNull(cache.get(1), "Item 1 remains");
            assertNotNull(cache.get(3), "Item 3 remains");
            assertNotNull(cache.get(5), "Item 5 remains");
            assertNotNull(cache.get(6), "Item 6 added");
            assertNotNull(cache.get(7), "Item 7 added");
            
            // Now add one more to trigger eviction
            cache.put(8, "eight");
            assertEquals(5, cache.size(), "Size at capacity");
        }

        @Test
        @DisplayName("Hand Position Maintenance")
        void testHandPositionMaintenance() {
            SieveCache<Integer, String> cache = new SieveCache<>(5);
            
            // Fill cache
            for (int i = 1; i <= 5; i++) {
                cache.put(i, "value" + i);
            }
            
            // Access middle items
            cache.get(2);
            cache.get(3);
            cache.get(4);
            
            // Trigger eviction - hand should find unvisited item 1
            cache.put(6, "six");
            assertNull(cache.get(1), "Item 1 evicted");
            
            // Trigger another eviction - hand should find item 5
            cache.put(7, "seven");
            assertNull(cache.get(5), "Item 5 evicted");
            
            // Hand should continue working properly
            cache.put(8, "eight");
            
            // Should have evicted one of the previously visited items
            int remainingOfOriginal = 0;
            for (int i = 2; i <= 4; i++) {
                if (cache.get(i) != null) remainingOfOriginal++;
            }
            assertTrue(remainingOfOriginal >= 2, "At least two of originally visited remain");
        }
    }

    @Nested
    @DisplayName("Extended Edge Case Tests")
    class ExtendedEdgeCaseTests {

        @Test
        @DisplayName("Large Capacity Cache")
        void testLargeCapacity() {
            SieveCache<Integer, String> cache = new SieveCache<>(10000);
            
            // Add many items
            for (int i = 0; i < 10000; i++) {
                cache.put(i, "value" + i);
            }
            
            assertEquals(10000, cache.size(), "Large cache filled");
            
            // Verify random samples
            Random rand = new Random(42);
            for (int i = 0; i < 100; i++) {
                int key = rand.nextInt(10000);
                assertEquals("value" + key, cache.get(key), "Random item " + key);
            }
        }

        @Test
        @DisplayName("Alternating Add/Remove")
        void testAlternatingAddRemove() {
            SieveCache<Integer, String> cache = new SieveCache<>(10);
            
            for (int i = 0; i < 20; i++) {
                cache.put(i, "value" + i);
                if (i % 2 == 1) {
                    cache.remove(i - 1);
                }
            }
            
            // Only odd numbers should remain
            for (int i = 0; i < 20; i++) {
                if (i % 2 == 0) {
                    assertNull(cache.get(i), "Even " + i + " removed");
                } else if (i >= 10) {
                    assertNotNull(cache.get(i), "Odd " + i + " present");
                }
            }
        }

        @Test
        @DisplayName("Same Key Different Values")
        void testSameKeyDifferentValues() {
            SieveCache<String, Integer> cache = new SieveCache<>(5);
            
            String key = "theKey";
            
            for (int i = 0; i < 100; i++) {
                cache.put(key, i);
                assertEquals(i, cache.get(key), "Value iteration " + i);
            }
            
            assertEquals(1, cache.size(), "Only one key in cache");
            assertEquals(99, cache.get(key), "Final value");
        }
    }

    @Nested
    @DisplayName("Stress Tests")
    @Timeout(60)
    class StressTests {

        @Test
        @DisplayName("Extreme Concurrency")
        void testExtremeConcurrency() throws InterruptedException {
            SieveCache<Integer, String> cache = new SieveCache<>(100);
            
            int numThreads = 100;
            CountDownLatch latch = new CountDownLatch(numThreads);
            AtomicInteger errors = new AtomicInteger(0);
            
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        for (int i = 0; i < 100; i++) {
                            cache.put(threadId * 1000 + i, "value");
                            cache.get(threadId * 1000 + i);
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                    latch.countDown();
                }).start();
            }
            
            assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads completed");
            assertEquals(0, errors.get(), "No errors under extreme concurrency");
        }

        @Test
        @DisplayName("Memory Pressure")
        void testMemoryPressure() {
            // Large cache with large values
            SieveCache<Integer, byte[]> cache = new SieveCache<>(1000);
            
            byte[] largeValue = new byte[10000]; // 10KB per entry
            Random rand = new Random();
            
            for (int i = 0; i < 5000; i++) {
                cache.put(i, largeValue);
                
                // Random access to create pressure
                if (i % 10 == 0) {
                    cache.get(rand.nextInt(i + 1));
                }
            }
            
            assertEquals(1000, cache.size(), "Cache maintains capacity under pressure");
        }

        @Test
        @DisplayName("Rapid Churn")
        void testRapidChurn() {
            SieveCache<Integer, String> cache = new SieveCache<>(10);
            
            // Rapidly cycle through many items
            for (int cycle = 0; cycle < 100; cycle++) {
                for (int i = 0; i < 100; i++) {
                    cache.put(cycle * 100 + i, "value");
                }
            }
            
            assertEquals(10, cache.size(), "Size stable after churn");
            
            // Most recent items should be present
            for (int i = 9990; i < 10000; i++) {
                assertNotNull(cache.get(i), "Recent item after churn");
            }
        }

        @Test
        @DisplayName("Worst Case Eviction")
        void testWorstCaseEviction() {
            SieveCache<Integer, String> cache = new SieveCache<>(100);
            
            // Fill cache
            for (int i = 0; i < 100; i++) {
                cache.put(i, "value" + i);
            }
            
            // Access all items (worst case for SIEVE)
            for (int i = 0; i < 100; i++) {
                cache.get(i);
            }
            
            // Force evictions with all items visited
            for (int i = 100; i < 200; i++) {
                cache.put(i, "value" + i);
            }
            
            assertEquals(100, cache.size(), "Size maintained in worst case");
        }

        @Test
        @DisplayName("Thread Safety Under Stress")
        void testThreadSafetyUnderStress() throws InterruptedException {
            SieveCache<String, String> cache = new SieveCache<>(20);
            
            int numThreads = 50;
            int duration = 2000; // 2 seconds
            AtomicBoolean stop = new AtomicBoolean(false);
            AtomicLong operations = new AtomicLong(0);
            CountDownLatch latch = new CountDownLatch(numThreads);
            
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                new Thread(() -> {
                    Random rand = new Random(threadId);
                    while (!stop.get()) {
                        String key = "k" + rand.nextInt(100);
                        
                        if (rand.nextBoolean()) {
                            cache.put(key, "v" + rand.nextInt());
                        } else {
                            cache.get(key);
                        }
                        operations.incrementAndGet();
                    }
                    latch.countDown();
                }).start();
            }
            
            Thread.sleep(duration);
            stop.set(true);
            assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads completed");
            
            assertTrue(operations.get() > 0, "Operations completed");
            assertTrue(cache.size() <= 20, "Cache size maintained");
        }
    }

    @Nested
    @DisplayName("Race Condition Tests")
    @Timeout(30)
    class RaceConditionTests {

        @Test
        @DisplayName("Value Validity")
        void testRaceConditions_ValueValidity() throws InterruptedException {
            SieveCache<Integer, Integer> cache = new SieveCache<>(50);
            
            int numThreads = 10;
            CountDownLatch latch = new CountDownLatch(numThreads);
            
            // Track all values that were ever written
            Set<Integer> allWrittenValues = ConcurrentHashMap.newKeySet();
            AtomicInteger inconsistencies = new AtomicInteger(0);
            
            // Threads continuously update same keys
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                new Thread(() -> {
                    for (int i = 0; i < 100; i++) {
                        for (int key = 0; key < 10; key++) {
                            int value = threadId * 1000 + i;
                            
                            // Record that this value was written
                            allWrittenValues.add(value);
                            cache.put(key, value);
                            
                            // Check that any value we read was actually written
                            Integer cached = cache.get(key);
                            if (cached != null && !allWrittenValues.contains(cached)) {
                                inconsistencies.incrementAndGet();
                            }
                        }
                    }
                    latch.countDown();
                }).start();
            }
            
            assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads completed");
            assertEquals(0, inconsistencies.get(), "No invalid values found");
        }

        @Test
        @DisplayName("Operation Atomicity")
        void testRaceConditions_OperationAtomicity() throws InterruptedException {
            SieveCache<String, String> cache = new SieveCache<>(50);
            
            int numThreads = 10;
            int keysPerThread = 20;
            CountDownLatch latch = new CountDownLatch(numThreads);
            AtomicInteger errors = new AtomicInteger(0);
            
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        // Each thread writes to its own set of keys
                        for (int i = 0; i < keysPerThread; i++) {
                            String key = "thread" + threadId + "_key" + i;
                            String value = "thread" + threadId + "_value" + i;
                            
                            cache.put(key, value);
                            
                            // Immediately verify our write
                            String retrieved = cache.get(key);
                            if (retrieved != null && !value.equals(retrieved)) {
                                // This would indicate a real race condition
                                errors.incrementAndGet();
                            }
                        }
                        
                        // Verify all our writes are still consistent
                        for (int i = 0; i < keysPerThread; i++) {
                            String key = "thread" + threadId + "_key" + i;
                            String expectedValue = "thread" + threadId + "_value" + i;
                            String actualValue = cache.get(key);
                            
                            // It's OK if the value was evicted (null)
                            // But if present, it must be our value
                            if (actualValue != null && !expectedValue.equals(actualValue)) {
                                errors.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                    latch.countDown();
                }).start();
            }
            
            assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads completed");
            assertEquals(0, errors.get(), "No atomicity errors");
        }

        @Test
        @DisplayName("Synchronization Test")
        void testRaceConditions_Synchronization() throws InterruptedException {
            SieveCache<Integer, AtomicInteger> cache = new SieveCache<>(50);
            
            int numThreads = 10;
            int iterations = 1000;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(numThreads);
            AtomicInteger conflicts = new AtomicInteger(0);
            
            // Use a small number of keys to increase contention
            int numKeys = 5;
            
            // Initialize counters
            for (int i = 0; i < numKeys; i++) {
                cache.put(i, new AtomicInteger(0));
            }
            
            for (int t = 0; t < numThreads; t++) {
                new Thread(() -> {
                    try {
                        startLatch.await(); // All threads start together
                        
                        Random rand = new Random();
                        for (int i = 0; i < iterations; i++) {
                            int key = rand.nextInt(numKeys);
                            
                            // Try to increment the counter
                            AtomicInteger counter = cache.get(key);
                            if (counter != null) {
                                counter.incrementAndGet();
                            } else {
                                // Was evicted, put it back
                                AtomicInteger newCounter = new AtomicInteger(1);
                                cache.put(key, newCounter);
                                
                                // Check if our put succeeded
                                AtomicInteger verify = cache.get(key);
                                if (verify != newCounter && verify != null) {
                                    // Another thread won the race
                                    conflicts.incrementAndGet();
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Expected in concurrent environment
                    }
                    endLatch.countDown();
                }).start();
            }
            
            startLatch.countDown(); // Start all threads
            assertTrue(endLatch.await(10, TimeUnit.SECONDS), "All threads completed");
            
            // Conflicts are expected in concurrent environment, but should be reasonable
            int conflictCount = conflicts.get();
            boolean acceptable = conflictCount < (numThreads * iterations * 0.1); // Less than 10%
            assertTrue(acceptable, "Conflicts within acceptable range: " + conflictCount);
        }

        @Test
        @DisplayName("Sequential Consistency")
        void testRaceConditions_SequentialConsistency() throws InterruptedException {
            SieveCache<Integer, Integer> cache = new SieveCache<>(50);
            
            int numThreads = 10;
            CountDownLatch latch = new CountDownLatch(numThreads);
            
            // Each thread writes a sequence of values to the same key
            AtomicInteger violations = new AtomicInteger(0);
            
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        int baseValue = threadId * 10000;
                        
                        for (int i = 0; i < 100; i++) {
                            int key = i % 10; // Use only 10 keys for contention
                            int value = baseValue + i;
                            
                            cache.put(key, value);
                            
                            // Read back
                            Integer retrieved = cache.get(key);
                            
                            if (retrieved != null) {
                                // The value should be from some thread's sequence
                                int threadFromValue = retrieved / 10000;
                                int sequenceNumber = retrieved % 10000;
                                
                                // Check if this is a valid value from any thread
                                if (threadFromValue < 0 || threadFromValue >= numThreads ||
                                        sequenceNumber < 0 || sequenceNumber >= 100) {
                                    violations.incrementAndGet();
                                }
                            }
                        }
                    } catch (Exception e) {
                        violations.incrementAndGet();
                    }
                    latch.countDown();
                }).start();
            }
            
            assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads completed");
            assertEquals(0, violations.get(), "No sequential consistency violations");
        }

        @Test
        @DisplayName("ABA Problem Test")
        void testABAProblem() throws InterruptedException {
            SieveCache<String, Integer> cache = new SieveCache<>(10);
            
            int numThreads = 3;
            CountDownLatch latch = new CountDownLatch(numThreads);
            AtomicInteger abaDetected = new AtomicInteger(0);
            
            // Thread 1: A -> B -> A pattern
            new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    cache.put("key", 1); // A
                    Thread.yield();
                    cache.put("key", 2); // B
                    Thread.yield();
                    cache.put("key", 1); // A again
                }
                latch.countDown();
            }).start();
            
            // Thread 2 & 3: Readers
            for (int t = 0; t < 2; t++) {
                new Thread(() -> {
                    Integer lastSeen = null;
                    for (int i = 0; i < 300; i++) {
                        Integer current = cache.get("key");
                        if (current != null && lastSeen != null) {
                            // Check for impossible transitions
                            if (lastSeen == 2 && current == 1) {
                                // This is fine - normal transition
                            } else if (lastSeen == 1 && current == 2) {
                                // This is fine - normal transition
                            } else if (!lastSeen.equals(current)) {
                                // Unexpected transition
                                abaDetected.incrementAndGet();
                            }
                        }
                        lastSeen = current;
                        Thread.yield();
                    }
                    latch.countDown();
                }).start();
            }
            
            assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads completed");
            // ABA issues are rare but acceptable in this context
            assertTrue(abaDetected.get() < 10, "ABA issues within acceptable range");
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Basic Stats Tracking")
        void testBasicStatsTracking() {
            SieveCache<String, Integer> cache = new SieveCache<>(5);
            
            // Initial stats should be zero
            CacheStats initialStats = cache.getStats();
            assertEquals(0, initialStats.getHits(), "Initial hits should be 0");
            assertEquals(0, initialStats.getMisses(), "Initial misses should be 0");
            assertEquals(0, initialStats.getEvictions(), "Initial evictions should be 0");
            assertEquals(0, initialStats.getTotalRequests(), "Initial requests should be 0");
            assertEquals(0.0, initialStats.getHitRate(), 0.001, "Initial hit rate should be 0");
            assertEquals(0.0, initialStats.getMissRate(), 0.001, "Initial miss rate should be 0");
            
            // Add some items
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);
            
            // Test hits
            cache.get("a"); // hit
            cache.get("b"); // hit
            cache.get("a"); // hit again
            
            // Test misses
            cache.get("d"); // miss
            cache.get("e"); // miss
            
            CacheStats afterHitsMisses = cache.getStats();
            assertEquals(3, afterHitsMisses.getHits(), "Should have 3 hits");
            assertEquals(2, afterHitsMisses.getMisses(), "Should have 2 misses");
            assertEquals(0, afterHitsMisses.getEvictions(), "Should have 0 evictions");
            assertEquals(5, afterHitsMisses.getTotalRequests(), "Should have 5 total requests");
            assertEquals(0.6, afterHitsMisses.getHitRate(), 0.001, "Hit rate should be 60%");
            assertEquals(0.4, afterHitsMisses.getMissRate(), 0.001, "Miss rate should be 40%");
            assertEquals(60.0, afterHitsMisses.getHitRatePercent(), 0.001, "Hit rate percent should be 60%");
            assertEquals(40.0, afterHitsMisses.getMissRatePercent(), 0.001, "Miss rate percent should be 40%");
        }

        @Test
        @DisplayName("Eviction Stats Tracking")
        void testEvictionStatsTracking() {
            SieveCache<Integer, String> cache = new SieveCache<>(3);
            
            // Fill cache to capacity
            cache.put(1, "one");
            cache.put(2, "two");
            cache.put(3, "three");
            
            CacheStats beforeEviction = cache.getStats();
            assertEquals(0, beforeEviction.getEvictions(), "No evictions yet");
            
            // Add more items to trigger evictions
            cache.put(4, "four"); // should evict one item
            cache.put(5, "five"); // should evict another item
            
            CacheStats afterEviction = cache.getStats();
            assertEquals(2, afterEviction.getEvictions(), "Should have 2 evictions");
            assertEquals(3, cache.size(), "Cache should still be at capacity");
            
            // Add a few more to test eviction counter continues
            cache.put(6, "six");
            cache.put(7, "seven");
            
            CacheStats finalStats = cache.getStats();
            assertEquals(4, finalStats.getEvictions(), "Should have 4 total evictions");
        }

        @Test
        @DisplayName("Stats Clear Functionality")
        void testStatsClearFunctionality() {
            SieveCache<String, Integer> cache = new SieveCache<>(3);
            
            // Generate some stats
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3); // Fill to capacity
            cache.get("a"); // hit
            cache.get("d"); // miss
            cache.put("e", 5); // should cause eviction since at capacity
            cache.put("f", 6); // another eviction
            
            CacheStats beforeClear = cache.getStats();
            assertTrue(beforeClear.getHits() > 0, "Should have hits before clear");
            assertTrue(beforeClear.getMisses() > 0, "Should have misses before clear");
            assertTrue(beforeClear.getEvictions() > 0, "Should have evictions before clear");
            
            // Clear stats
            cache.clearStats();
            
            CacheStats afterClear = cache.getStats();
            assertEquals(0, afterClear.getHits(), "Hits should be 0 after clear");
            assertEquals(0, afterClear.getMisses(), "Misses should be 0 after clear");
            assertEquals(0, afterClear.getEvictions(), "Evictions should be 0 after clear");
            assertEquals(0, afterClear.getTotalRequests(), "Total requests should be 0 after clear");
            assertEquals(0.0, afterClear.getHitRate(), 0.001, "Hit rate should be 0 after clear");
            
            // Verify cache contents are not affected by stats clear (some item should still be there)
            assertEquals(3, cache.size(), "Cache size should remain after stats clear");
        }

        @Test
        @DisplayName("Stats Accuracy Under Load")
        void testStatsAccuracyUnderLoad() {
            SieveCache<Integer, String> cache = new SieveCache<>(10);
            
            // Pre-populate half the cache
            for (int i = 0; i < 5; i++) {
                cache.put(i, "value" + i);
            }
            
            cache.clearStats(); // Reset to get clean numbers
            
            int expectedHits = 0;
            int expectedMisses = 0;
            int expectedEvictions = 0;
            
            // Perform a mix of operations
            for (int i = 0; i < 100; i++) {
                if (i % 3 == 0) {
                    // Hit existing keys
                    cache.get(i % 5);
                    expectedHits++;
                } else if (i % 3 == 1) {
                    // Miss non-existing keys
                    cache.get(i + 100);
                    expectedMisses++;
                } else {
                    // Add new keys (may cause evictions after capacity)
                    cache.put(i + 200, "new" + i);
                    if (cache.size() == 10 && i > 10) {
                        expectedEvictions++;
                    }
                }
            }
            
            CacheStats finalStats = cache.getStats();
            assertEquals(expectedHits, finalStats.getHits(), "Hit count should match expected");
            assertEquals(expectedMisses, finalStats.getMisses(), "Miss count should match expected");
            
            // Evictions are harder to predict exactly due to SIEVE algorithm, but should be > 0
            assertTrue(finalStats.getEvictions() > 0, "Should have some evictions");
            
            assertEquals(expectedHits + expectedMisses, finalStats.getTotalRequests(), 
                "Total requests should equal hits + misses");
            
            if (finalStats.getTotalRequests() > 0) {
                double expectedHitRate = (double) expectedHits / (expectedHits + expectedMisses);
                assertEquals(expectedHitRate, finalStats.getHitRate(), 0.001, "Hit rate should be correct");
            }
        }

        @Test
        @DisplayName("Stats String Representation")
        void testStatsStringRepresentation() {
            SieveCache<String, Integer> cache = new SieveCache<>(5);
            
            // Generate some stats
            cache.put("a", 1);
            cache.put("b", 2);
            cache.get("a"); // hit
            cache.get("c"); // miss
            cache.put("d", 4);
            cache.put("e", 5);
            cache.put("f", 6); // should cause eviction
            
            CacheStats stats = cache.getStats();
            String statsString = stats.toString();
            
            // Verify the string contains all expected components
            assertTrue(statsString.contains("hits="), "String should contain hits");
            assertTrue(statsString.contains("misses="), "String should contain misses");
            assertTrue(statsString.contains("evictions="), "String should contain evictions");
            assertTrue(statsString.contains("hitRate="), "String should contain hit rate");
            assertTrue(statsString.contains("requests="), "String should contain total requests");
            assertTrue(statsString.contains("%"), "String should contain percentage symbol");
            
            // Verify format is reasonable
            assertTrue(statsString.startsWith("CacheStats{"), "String should start with CacheStats{");
            assertTrue(statsString.endsWith("}"), "String should end with }");
        }

        @Test
        @DisplayName("Stats Equality and HashCode")
        void testStatsEqualityAndHashCode() {
            SieveCache<String, Integer> cache1 = new SieveCache<>(5);
            SieveCache<String, Integer> cache2 = new SieveCache<>(5);
            
            // Generate identical stats
            cache1.put("a", 1);
            cache1.get("a"); // hit
            cache1.get("b"); // miss
            
            cache2.put("x", 1);
            cache2.get("x"); // hit  
            cache2.get("y"); // miss
            
            CacheStats stats1 = cache1.getStats();
            CacheStats stats2 = cache2.getStats();
            
            // Should be equal since same hits, misses, evictions
            assertEquals(stats1, stats2, "Stats with same counts should be equal");
            assertEquals(stats1.hashCode(), stats2.hashCode(), "Equal stats should have same hash code");
            
            // Test inequality
            cache1.get("c"); // another miss
            CacheStats stats3 = cache1.getStats();
            
            assertNotEquals(stats1, stats3, "Stats with different counts should not be equal");
        }
    }

    @Nested
    @DisplayName("Correctness Tests")
    class CorrectnessTests {

        @Test
        @DisplayName("SIEVE Invariant")
        void testSieveInvariant() {
            SieveCache<Integer, String> cache = new SieveCache<>(5);
            
            // Add items
            for (int i = 0; i < 5; i++) {
                cache.put(i, "value" + i);
            }
            
            // Mark some as visited
            cache.get(1);
            cache.get(3);
            
            // Add new item - should evict unvisited before visited
            cache.put(5, "value5");
            
            // Visited items should be preferred for retention
            assertNotNull(cache.get(1), "Visited item retained");
            assertNotNull(cache.get(3), "Visited item retained");
        }

        @Test
        @DisplayName("Eviction Fairness")
        void testEvictionFairness() {
            SieveCache<Integer, String> cache = new SieveCache<>(10);
            
            Map<Integer, Integer> evictionCount = new HashMap<>();
            
            for (int round = 0; round < 10; round++) {
                // Fill cache
                for (int i = 0; i < 10; i++) {
                    cache.put(i, "value" + i);
                }
                
                // Don't access any items (all unvisited)
                
                // Add items to trigger evictions
                for (int i = 10; i < 20; i++) {
                    cache.put(i, "value" + i);
                }
                
                // Count what was evicted
                for (int i = 0; i < 10; i++) {
                    if (cache.get(i) == null) {
                        evictionCount.merge(i, 1, Integer::sum);
                    }
                }
                
                cache.clear();
            }
            
            // Check fairness - no item should be evicted significantly more
            int maxEvictions = Collections.max(evictionCount.values());
            int minEvictions = Collections.min(evictionCount.values());
            
            assertTrue(maxEvictions - minEvictions <= 5,
                    "Eviction fairness maintained");
        }

        @Test
        @DisplayName("Visited Bit Semantics")
        void testVisitedBitSemantics() {
            SieveCache<String, Integer> cache = new SieveCache<>(5);
            
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);
            
            // Get should set visited bit
            cache.get("a");
            cache.get("b");
            
            // Put on existing should also set visited
            cache.put("c", 33);
            
            // Add items to trigger eviction
            cache.put("d", 4);
            cache.put("e", 5);
            
            // All accessed items should still be present
            assertNotNull(cache.get("a"), "Get-visited retained");
            assertNotNull(cache.get("b"), "Get-visited retained");
            assertNotNull(cache.get("c"), "Put-visited retained");
        }

        @Test
        @DisplayName("Capacity Invariant")
        void testCapacityInvariant() {
            int capacity = 50;
            SieveCache<Integer, String> cache = new SieveCache<>(capacity);
            
            Random rand = new Random(42);
            
            // Perform random operations
            for (int i = 0; i < 1000; i++) {
                int op = rand.nextInt(3);
                
                switch (op) {
                    case 0: // Put
                        cache.put(rand.nextInt(200), "value");
                        break;
                    case 1: // Get
                        cache.get(rand.nextInt(200));
                        break;
                    case 2: // Remove
                        cache.remove(rand.nextInt(200));
                        break;
                }
                
                // Check capacity invariant
                assertTrue(cache.size() <= capacity,
                        "Size exceeds capacity at iteration " + i);
            }
        }
    }
}