# Simple Cache (Work In Progress)

A simple annotation-driven caching solution for Apache Karaf that combines simplicity with enterprise-grade functionality.
Built on OSGi architecture, this modular caching solution enables developers to add caching capabilities with minimal code changes.

## Key Features

- **Zero-Configuration Caching**: Add caching to any method with a single annotation
- **Automatic Cache Management**: Smart cache creation, key generation, and lifecycle management
- **Dual Cache Strategies**: In-memory for single instances, clustered (Infinispan) for distributed environments
- **Programmatic API**: Full control when you need it, with a clean and intuitive interface
- **OSGi Integration**: Native support for dynamic service registration and dependency injection
- **Production Ready**: Comprehensive test coverage and battle-tested in enterprise environments

## Project Structure

```
cache/
├── cache-api/          # Core interfaces and annotations
├── cache-core/         # Implementation with in-memory and clustered caching
├── cache-features/     # Karaf feature definitions for easy deployment
├── cache-itests/       # Comprehensive integration tests
└── cache-samples/      # Working examples and usage patterns
```

## The Power of Annotations

Transform any expensive method call into a cached operation with zero boilerplate:

### Before (Manual Cache Management)
```java
public class ExpensiveService {
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    
    public String computeData(String input) {
        String cacheKey = "compute_" + input;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        
        // Expensive computation
        String result = performComplexCalculation(input);
        cache.put(cacheKey, result);
        return result;
    }
}
```

### After (Annotation-Driven)
```java
public class ExpensiveService {
    @CacheResult(cacheName = "computeCache")
    public String computeData(String input) {
        // Just your business logic - caching is handled automatically
        return performComplexCalculation(input);
    }
    
    @CacheInvalidate(cacheName = "computeCache")
    public void clearCache() {
        // Cache automatically cleared when this method is called
    }
}
```

### What Happens Behind the Scenes
- **Automatic Key Generation**: Method parameters are intelligently combined to create unique cache keys
- **Cache Creation**: If the cache doesn't exist, it's created with sensible defaults
- **Transparent Interception**: Method calls are intercepted via dynamic proxies
- **Lifecycle Management**: Caches are automatically cleaned up when services are unregistered

## 🛠 Quick Start

### 1. Installation

Deploy the cache feature in your Karaf container:

```bash
# Add the features repository
feature:repo-add mvn:org.jahia.features.cache/cache-features/1.0.0-SNAPSHOT/xml/features

# Install the cache
feature:install cache
```

### 2. Add Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.jahia.features.cache</groupId>
        <artifactId>cache-api</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### 2. Annotation-Based Caching (Recommended)

The simplest way to cache method's result is using annotations. Just annotate your interface methods in your OSGi components:

```java
public interface BasicCacheSampleService {
    String CACHE_NAME = "sample-basic-cache";

    @CacheResult(cacheName = CACHE_NAME)
    String getValue();

    @CacheInvalidate(cacheName = CACHE_NAME)
    void setValue(String value);

}

@Component(service = BasicCacheSampleService.class, immediate = true)
public class BasicCacheSampleServiceImpl implements BasicCacheSampleService {
    private String value = "default";

    @Override
    public String getValue() {
        return "Value at " + System.currentTimeMillis() + " is: " + value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }
}
```

**That's it!** The cache automatically discovers your annotated interface when your OSGi component is activated and replaces the service with a cached proxy.

### 3. Programmatic Cache Usage

When you need fine-grained control, use the programmatic API:

```java
@Component
public class AdvancedCacheService {
    
    @Reference
    private CacheService cacheService;
    
    public void demonstrateProgrammaticUsage() {
        // Create a custom cache with specific configuration
        Cache<String> myCache = cacheService.create(
            "customCache",
            CacheConfig.create()
                .maxEntries(5000)
                .timeToLive(1800) // 30 minutes
                .build(),
            String.class
        );
        
        // Direct cache operations
        myCache.put("key1", "value1");
        String value = myCache.get("key1");
        
        // Check cache entry with metadata
        CacheEntry<String> entry = myCache.getEntry("key1");
        if (entry != null) {
            System.out.println("Value: " + entry.getValue());
            System.out.println("Created: " + entry.getCreationTime());
            System.out.println("TTL: " + entry.getTimeToLive());
        }
        
        // Cache statistics and management
        System.out.println("Cache size: " + myCache.size());
        myCache.clear();
    }
    
    public User getUser(String id) {
        try {
            // Get an existing cache
            Cache<User> userCache = cacheService.get("userCache", User.class);
            
            // Perform operations
            User user = userCache.get(id);
            if (user == null) {
                user = loadUserFromDatabase("123");
                userCache.put("user:123", user);
            }
            return user;
        } catch (CacheNotFoundException e) {
            // Handle cache not found
            System.err.println("Cache not found: " + e.getMessage());
        }
    }
}
```

## Advanced Configuration

### Cache Configuration Options

```java
CacheConfig config = CacheConfig.create()
    .maxEntries(10000)      // Maximum number of entries
    .timeToLive(3600)       // TTL in seconds (1 hour)
    .build();

Cache<String> cache = cacheService.create("myCache", config, String.class);
```

### Clustered vs In-Memory Caching

The framework automatically detects the environment:

- **In-Memory Cache**: Used by default, perfect for single-instance applications
- **Clustered Cache**: Automatically enabled when Infinispan features are available, ideal for distributed systems

## Architecture Details

### Cache Key Generation
- Automatic key generation based on method signature and parameters
- Handles complex objects via serialization
- Collision-resistant and deterministic

### Proxy-Based Interception  
- Uses JDK dynamic proxies for service interception
- Seamless integration with OSGi service registration
- Minimal performance overhead

### Cache Lifecycle
- Caches are created on-demand
- Automatic cleanup when services are unregistered
- Graceful handling of cache misses and errors

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

Licensed under the Apache License 2.0. See [LICENSE](LICENSE) file for details.

## Support

- **Issues**: Report bugs and feature requests via GitHub Issues
- **Documentation**: Check the `cache-samples` module for working examples
- **Community**: Join our developer community for discussions

---

**Built for Simplicity, Designed for Scale**

This caching framework eliminates the complexity of cache management while providing enterprise-grade performance and reliability. Whether you're building a simple application or a distributed system, the annotation-driven approach ensures your caching logic remains clean, maintainable, and powerful.
