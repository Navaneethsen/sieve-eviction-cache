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

import org.cache.common.AbstractCache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A SIEVE cache implementation based on the NSDI'24 paper.
 * SIEVE is an efficient eviction algorithm that improves upon LRU
 * by using a simpler data structure while maintaining comparable performance.
 *
 * @param <K> The type of keys maintained by this cache
 * @param <V> The type of mapped values
 */
public class SieveCache<K, V> extends AbstractCache<K, V> {
    
    /**
     * Node class for the doubly-linked queue.
     * Each node represents a cache entry with metadata for the SIEVE algorithm.
     */
    private static class Node<K, V> {
        final K key;
        volatile V value;
        final AtomicBoolean visited;  // SIEVE algorithm's visited bit
        volatile Node<K, V> prev;     // points toward older nodes (toward tail)
        volatile Node<K, V> next;     // points toward newer nodes (toward head)
        
        Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.visited = new AtomicBoolean(false);
        }
    }
    
    private final ConcurrentHashMap<K, Node<K, V>> map;
    private volatile Node<K, V> head;                      // Newest objects
    private volatile Node<K, V> tail;                      // Oldest objects
    private final AtomicReference<Node<K, V>> hand;        // SIEVE eviction pointer
    private final ReentrantLock evictionLock;              // Serializes structure modifications
    
    /**
     * Creates a new SIEVE cache with the specified capacity.
     *
     * @param capacity the maximum number of entries the cache can hold
     * @throws IllegalArgumentException if capacity is not positive
     */
    public SieveCache(int capacity) {
        super(capacity);
        this.map = new ConcurrentHashMap<>(capacity);
        this.hand = new AtomicReference<>();
        this.evictionLock = new ReentrantLock();
    }
    
    /**
     * Retrieves a value from the cache.
     * This operation is lock-free for cache hits, providing excellent scalability.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the key, or null if not present
     */
    @Override
    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node != null) {
            // Cache hit - just set visited bit (lock-free operation)
            node.visited.set(true);
            recordHit();
            return node.value;
        }
        recordMiss();
        return null;
    }
    
    /**
     * Inserts or updates a key-value pair in the cache.
     * New objects are always inserted at the head of the queue.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @throws NullPointerException if key or value is null
     */
    @Override
    public void put(K key, V value) {
        validateKeyValue(key, value);
        
        // Check if key already exists
        Node<K, V> existing = map.get(key);
        if (existing != null) {
            existing.value = value;
            existing.visited.set(true);
            return;
        }
        
        // Need to add new entry
        evictionLock.lock();
        try {
            // Double-check after acquiring lock
            existing = map.get(key);
            if (existing != null) {
                existing.value = value;
                existing.visited.set(true);
                return;
            }
            
            // Evict if at capacity
            while (map.size() >= capacity) {
                if (!evict()) {
                    // Failed to evict (shouldn't happen in normal operation)
                    System.err.println("Warning: Failed to evict, cache at capacity");
                    break;
                }
            }
            
            // Create and insert new node at head
            Node<K, V> newNode = new Node<>(key, value);
            insertAtHead(newNode);
            map.put(key, newNode);
            
        } finally {
            evictionLock.unlock();
        }
    }
    
    /**
     * Removes an entry from the cache.
     *
     * @param key the key of the entry to remove
     * @return the value that was associated with the key, or null if not present
     */
    @Override
    public V remove(K key) {
        evictionLock.lock();
        try {
            Node<K, V> node = map.remove(key);
            if (node != null) {
                removeNode(node);
                return node.value;
            }
            return null;
        } finally {
            evictionLock.unlock();
        }
    }
    
    /**
     * Returns the current size of the cache.
     *
     * @return the number of entries currently in the cache
     */
    @Override
    public int size() {
        return map.size();
    }
    
    /**
     * Clears all entries from the cache.
     */
    @Override
    public void clear() {
        evictionLock.lock();
        try {
            map.clear();
            head = null;
            tail = null;
            hand.set(null);
            clearStats();
        } finally {
            evictionLock.unlock();
        }
    }
    
    /**
     * Checks if the cache contains the specified key.
     *
     * @param key the key to check for
     * @return true if the cache contains the key, false otherwise
     */
    @Override
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }
    
    /**
     * The core SIEVE eviction algorithm.
     * Moves the hand from tail towards head, evicting the first unvisited object.
     * Visited objects have their visited bit reset and are retained.
     *
     * @return true if an object was evicted, false otherwise
     */
    private boolean evict() {
        Node<K, V> candidate = hand.get();
        
        // If hand is null or invalid, start from tail
        if (candidate == null || !isNodeValid(candidate)) {
            candidate = tail;
            hand.set(tail);
        }
        
        // Safety check - if no nodes, nothing to evict
        if (candidate == null) {
            return false;
        }
        
        // Track starting position to detect full traversal
        Node<K, V> startNode = candidate;
        int iterations = 0;
        int maxIterations = map.size() * 3; // Safeguard against infinite loops. By the way, this is a thumb-sucked value.
        
        // Find an object to evict
        do {
            iterations++;
            
            // Safety check to prevent infinite loops
            if (iterations > maxIterations) {
                System.err.println("Warning: Max iterations reached in evict()");
                // Force evict the current candidate
                map.remove(candidate.key);
                removeNode(candidate);
                recordEviction();
                hand.set(tail); // Reset hand
                return true;
            }
            
            if (candidate.visited.compareAndSet(true, false)) {
                // Object was visited - retain it and move to prev
                Node<K, V> prevCandidate = candidate.prev;
                
                // If we've reached the head, wrap around to tail
                if (prevCandidate == null) {
                    candidate = tail;
                } else {
                    candidate = prevCandidate;
                }
            } else {
                // Found unvisited object - evict it
                Node<K, V> toEvict = candidate;
                Node<K, V> currHand = candidate.prev;
                
                // If evicting the last node before wrapping, set hand to tail
                if (currHand == null) {
                    currHand = tail;
                }
                
                // Remove from map
                map.remove(toEvict.key);
                
                // Remove from linked list
                removeNode(toEvict);
                
                // Update hand position
                hand.set(currHand);
                recordEviction();
                return true;
            }
            
        } while (candidate != startNode && candidate != null);
        
        // If we've traversed everything and found nothing to evict,
        // force evict the tail (oldest item)
        if (tail != null) {
            Node<K, V> toEvict = tail;
            Node<K, V> newTail = tail.prev;
            
            map.remove(toEvict.key);
            removeNode(toEvict);
            hand.set(newTail);
            recordEviction();
            return true;
        }
        
        return false;
    }
    
    /**
     * Inserts a node at the head of the queue (most recently used position).
     *
     * @param node the node to insert
     */
    private void insertAtHead(Node<K, V> node) {
        node.next = head;
        node.prev = null;
        
        if (head != null) {
            head.prev = node;
        }
        head = node;
        
        if (tail == null) {
            tail = node;
        }
    }
    
    /**
     * Removes a node from the doubly-linked list.
     * Updates head, tail, and hand pointers as necessary.
     *
     * @param node the node to remove
     */
    private void removeNode(Node<K, V> node) {
        // Update hand if it points to the node being removed
        if (hand.get() == node) {
            hand.set(node.prev != null ? node.prev : tail);
        }
        
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            // node is head
            if (node.next != null) {
                node.next.prev = null;
                head = node.next;
            } else {
                // node is the only element
                head = null;
            }
        }
        
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            if (node.prev != null) {
                node.prev.next = null;
                tail = node.prev;
            } else {
                // node is the only element
                tail = null;
            }
        }
        
        // Clear pointers to help GC
        node.prev = null;
        node.next = null;
    }
    
    /**
     * Checks if a node is still valid (exists in the cache).
     *
     * @param node the node to check
     * @return true if the node is valid, false otherwise
     */
    private boolean isNodeValid(Node<K, V> node) {
        return node != null && map.containsKey(node.key);
    }
    
    /**
     * Returns a detailed string representation of the cache state.
     * Useful for debugging and testing.
     *
     * @return a string representation of the cache
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SieveCache State:\n");
        sb.append("Size: ").append(size()).append("/").append(capacity).append("\n");
        sb.append("Stats: ").append(getStats()).append("\n");
        
        sb.append("Head: ").append(head != null ? head.key : "null").append("/").append(head != null ? head.value : "null").append("\n");
        sb.append("Tail: ").append(tail != null ? tail.key : "null").append("/").append(tail != null ? tail.value : "null").append("\n");
        sb.append("Hand: ").append(hand.get() != null ? hand.get().key : "null").append("/").append(hand.get() != null ? hand.get().value : "null").append("\n");
        
        sb.append("Cache Elements (from head to tail):\n");
        Node<K, V> current = head;
        while (current != null) {
            sb.append("[Key: ").append(current.key)
                    .append(", Value: ").append(current.value)
                    .append(", Visited: ").append(current.visited.get())
                    .append(", Prev: ").append(current.prev != null ? current.prev.key : "null")
                    .append(", Next: ").append(current.next != null ? current.next.key : "null")
                    .append("]\n");
            current = current.next;
        }
        return sb.toString();
    }
}