# SIEVE Eviction Cache

A high-performance, thread-safe Java implementation of the SIEVE cache eviction algorithm with comprehensive statistics tracking.

For more details, please read through my medium post [SIEVE: The Surprisingly Simple Cache Eviction Algorithm](https://navaneethsen.medium.com/sieve-the-surprisingly-simple-cache-eviction-algorithm-525b2b7ba3b2)

## References
- [Zhang et al., 2024 — SIEVE is Simpler than LRU: An Efficient Turn-Key Eviction Algorithm for Web Caches](https://junchengyang.com/publication/nsdi24-SIEVE.pdf)
- [SIEVE is simpler than LRU - Website](https://cachemon.github.io/SIEVE-website/blog/2023/12/17/sieve-is-simpler-than-lru/)
- [ChatGPT - Claude](https://claude.ai) for generating code and documentation

## Features

- **SIEVE Algorithm**: State-of-the-art cache eviction algorithm that combines the simplicity of FIFO with the effectiveness of LRU
- **Thread-Safe**: Concurrent access support for multi-threaded applications
- **Statistics Tracking**: Comprehensive metrics including hit rate, miss rate, eviction count, and more
- **Generic Types**: Full support for any key-value types with Java generics
- **Builder Pattern**: Easy cache configuration and instantiation
- **Comprehensive Testing**: Many JUnit tests covering different scenarios including race conditions and stress testing

## Quick Start

### Maven Dependency

Add this to your `pom.xml`:

```xml
<dependency>
    <groupId>org.sen.cache</groupId>
    <artifactId>sieve-eviction-cache</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Basic Usage

```java
import org.cache.sieve.SieveCache;

// Create a cache with capacity of 1000
SieveCache<String, String> cache = new SieveCache<>(1000);

// Put some values
cache.put("key1", "value1");
cache.put("key2", "value2");

// Get values
String value = cache.get("key1"); // Returns "value1"
String missing = cache.get("key3"); // Returns null

// Remove values
String removed = cache.remove("key1"); // Returns "value1"

// Clear the cache
cache.clear();
```

### Statistics Tracking

```java
import org.cache.common.CacheStats;

// Perform some operations
cache.put("key1", "value1");
cache.get("key1"); // hit
cache.get("key2"); // miss

// Get statistics
CacheStats stats = cache.getStats();
System.out.println("Hit rate: " + stats.getHitRatePercent() + "%");
System.out.println("Total requests: " + stats.getTotalRequests());
System.out.println("Evictions: " + stats.getEvictions());

// Reset statistics
cache.clearStats();
```

## SIEVE Algorithm

The SIEVE algorithm is a simple and effective eviction policy that:

1. **Tracks Access**: Maintains a "visited" bit for each cache entry
2. **FIFO-Based Eviction**: Uses a hand pointer that moves in FIFO order
3. **Second Chance**: Gives accessed items a second chance by clearing their visited bit
4. **Optimal Performance**: Combines simplicity of FIFO with effectiveness approaching LRU


## API Reference

### Core Methods

| Method | Description |
|--------|-------------|
| `put(K key, V value)` | Store a key-value pair in the cache |
| `get(K key)` | Retrieve a value by key (returns null if not found) |
| `remove(K key)` | Remove and return a value by key |
| `clear()` | Remove all entries from the cache |
| `size()` | Get the current number of entries |
| `getCapacity()` | Get the maximum capacity |

### Statistics Methods

| Method | Description |
|--------|-------------|
| `getStats()` | Get current cache statistics |
| `clearStats()` | Reset all statistics counters |

### CacheStats Properties

| Property | Description |
|----------|-------------|
| `getHits()` | Number of successful cache retrievals |
| `getMisses()` | Number of cache misses |
| `getEvictions()` | Number of entries evicted |
| `getTotalRequests()` | Total number of get operations |
| `getHitRate()` | Hit rate as a decimal (0.0 to 1.0) |
| `getHitRatePercent()` | Hit rate as a percentage |

## Performance Characteristics

- **Time Complexity**: O(1) average for all operations (put, get, remove)
- **Space Complexity**: O(capacity) 
- **Concurrency**: Thread-safe with optimized locking strategy
- **Throughput**: >100K operations/second in benchmarks

## Building and Testing

### Requirements

- Java 17 or higher
- Maven 3.6 or higher

### Build

```bash
mvn clean compile
```

### Run Tests

```bash
mvn test
```

The project includes 53 comprehensive tests covering:
- Basic functionality
- Eviction scenarios  
- Edge cases
- Concurrency and race conditions
- Performance and stress testing
- Statistics accuracy


## Thread Safety

The SIEVE cache implementation is fully thread-safe:

- **Concurrent Reads**: Multiple threads can read simultaneously
- **Concurrent Writes**: Thread-safe updates with minimal contention
- **Mixed Operations**: Safe concurrent mix of reads, writes, and evictions
- **Statistics**: Thread-safe statistics tracking


## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.


## Support

If you have questions or need help:

1. Check the [Issues](https://github.com/Navaneethsen/sieve-eviction-cache/issues) page
2. Create a new issue if your question isn't answered

---

**⭐ Star this repository if you find it useful!**
