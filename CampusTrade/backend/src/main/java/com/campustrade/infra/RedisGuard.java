package com.campustrade.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 守卫：幂等与限流共用的读写入口。
 * <p>
 * 和 CacheService 同样的取舍——Redis 不可用时降级为进程内计数，
 * 保证"本地没装 Redis 也能把项目跑起来"，代价是降级期间多实例之间不共享状态（单机部署无影响）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisGuard {

    /** INCR 并仅在首次设置过期时间，避免 key 永远不过期 */
    private static final DefaultRedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>(
            "local v = redis.call('INCR', KEYS[1]) "
                    + "if v == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end "
                    + "return v",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    private final Map<String, LocalEntry> localStore = new ConcurrentHashMap<>();

    private volatile boolean redisAvailable = true;

    /** SET NX：抢到锁返回 true */
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        if (redisAvailable) {
            try {
                return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, ttl));
            } catch (Exception e) {
                degrade(e);
            }
        }
        final boolean[] acquired = {false};
        localStore.compute(key, (k, old) -> {
            long now = System.currentTimeMillis();
            if (old == null || old.expireAt <= now) {
                acquired[0] = true;
                return new LocalEntry(value, now + ttl.toMillis());
            }
            return old;
        });
        return acquired[0];
    }

    public String get(String key) {
        if (redisAvailable) {
            try {
                return redisTemplate.opsForValue().get(key);
            } catch (Exception e) {
                degrade(e);
            }
        }
        LocalEntry entry = localStore.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt <= System.currentTimeMillis()) {
            localStore.remove(key);
            return null;
        }
        return entry.value;
    }

    public void set(String key, String value, Duration ttl) {
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(key, value, ttl);
                return;
            } catch (Exception e) {
                degrade(e);
            }
        }
        localStore.put(key, new LocalEntry(value, System.currentTimeMillis() + ttl.toMillis()));
    }

    /** 删除 key（业务失败时释放幂等锁） */
    public void delete(String key) {
        if (redisAvailable) {
            try {
                redisTemplate.delete(key);
                return;
            } catch (Exception e) {
                degrade(e);
            }
        }
        localStore.remove(key);
    }

    /** 自增计数，返回自增后的值（窗口内第几次请求） */
    public long increment(String key, Duration ttl) {
        if (redisAvailable) {
            try {
                Long value = redisTemplate.execute(INCR_WITH_TTL,
                        Collections.singletonList(key), String.valueOf(ttl.toMillis()));
                return value == null ? 0L : value;
            } catch (Exception e) {
                degrade(e);
            }
        }
        long now = System.currentTimeMillis();
        LocalEntry entry = localStore.compute(key, (k, old) -> {
            if (old == null || old.expireAt <= now) {
                return new LocalEntry("1", now + ttl.toMillis());
            }
            return new LocalEntry(String.valueOf(Long.parseLong(old.value) + 1), old.expireAt);
        });
        return Long.parseLong(entry.value);
    }

    private void degrade(Exception e) {
        if (redisAvailable) {
            log.warn("Redis 不可用，幂等/限流降级为进程内实现：{}", e.getMessage());
        }
        redisAvailable = false;
    }

    private static final class LocalEntry {
        private final String value;
        private final long expireAt;

        private LocalEntry(String value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}