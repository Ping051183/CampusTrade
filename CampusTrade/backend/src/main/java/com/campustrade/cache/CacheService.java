package com.campustrade.cache;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 统一缓存组件
 * <p>
 * 优先使用 Redis；当本机未安装/未启动 Redis 时自动降级为进程内缓存，
 * 保证项目在没有 Redis 的电脑上也能正常启动和运行（只是多实例部署时缓存不共享）。
 */
@Slf4j
@Component
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /** 本地降级缓存：key -> 值 + 过期时间 */
    private final Map<String, LocalEntry> localCache = new ConcurrentHashMap<>();

    /** Redis 是否可用（启动时探测一次，运行中出现异常也会自动降级） */
    private volatile boolean redisAvailable = false;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.ping();
            redisAvailable = true;
            log.info("Redis 连接成功，缓存将使用 Redis");
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("Redis 不可用（{}），已自动降级为本地内存缓存。安装并启动 Redis 后重启应用即可启用。", e.getMessage());
        }
    }

    /**
     * 读取缓存
     *
     * @return 缓存值，不存在或类型不匹配时返回 null
     */
    public <T> T get(String key, Class<T> type) {
        if (redisAvailable) {
            try {
                Object value = redisTemplate.opsForValue().get(key);
                return convert(value, type);
            } catch (Exception e) {
                degrade(e);
            }
        }
        LocalEntry entry = localCache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expired()) {
            localCache.remove(key);
            return null;
        }
        return convert(entry.value, type);
    }

    /**
     * 写入缓存
     */
    public void put(String key, Object value, Duration ttl) {
        if (value == null) {
            return;
        }
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(key, value, ttl);
                return;
            } catch (Exception e) {
                degrade(e);
            }
        }
        localCache.put(key, new LocalEntry(value, System.nanoTime() + ttl.toNanos()));
    }

    /**
     * 读取缓存，未命中则执行 loader 并回填
     */
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        T cached = get(key, type);
        if (cached != null) {
            return cached;
        }
        T value = loader.get();
        if (value != null) {
            put(key, value, ttl);
        }
        return value;
    }

    /**
     * 删除单个缓存
     */
    public void evict(String key) {
        localCache.remove(key);
        if (redisAvailable) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                degrade(e);
            }
        }
    }

    /**
     * 当前是否使用 Redis（便于测试/排障）
     */
    public boolean isRedisAvailable() {
        return redisAvailable;
    }

    @SuppressWarnings("unchecked")
    private <T> T convert(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    private void degrade(Exception e) {
        if (redisAvailable) {
            redisAvailable = false;
            log.warn("Redis 访问异常（{}），已自动降级为本地内存缓存", e.getMessage());
        }
    }

    /**
     * 本地缓存条目，带纳秒级过期时间
     */
    private static final class LocalEntry {
        private final Object value;
        private final long expireAtNanos;

        private LocalEntry(Object value, long expireAtNanos) {
            this.value = value;
            this.expireAtNanos = expireAtNanos;
        }

        private boolean expired() {
            return System.nanoTime() > expireAtNanos;
        }
    }
}