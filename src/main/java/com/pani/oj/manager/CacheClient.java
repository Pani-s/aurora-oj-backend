package com.pani.oj.manager;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pani.oj.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author Pani
 * @date Created in 2024/3/25 20:51
 * @description 处理 redis 缓存
 */
@Component
@Slf4j
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 创建一个指定大小的线程池，可控制线程的最大并发数，超出的线程会在LinkedBlockingQueue阻塞队列中等待
     */
    //    private static final ExecutorService CACHE_REBUILD_EXECUTOR =
    //            Executors.newFixedThreadPool(10);
    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 存
     * 将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
     *
     * @param key   key
     * @param value 序列化的对象
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 取得 单对象
     */
    public <R> R get(String key,Class<R> type) {
        String json = stringRedisTemplate.opsForValue().get(key);
        //.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            log.info("缓存存在，直接返回。");
            //存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        return null;
    }

    /**
     * 取得 list
     */
    public <T> List<T> getList(String key,Class<T> type) {
        String json = stringRedisTemplate.opsForValue().get(key);
        //.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            log.info("缓存存在，直接返回。");
            //存在，直接返回
            List<T> list = JSONUtil.toBean(json, new TypeReference<List<T>>() {
            }, true);
            return list;
        }
        return null;
    }

    /**
     * 删除key
     *
     * @param key
     */
    public void deleteKey(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 删除批量key  相同前缀
     */
    public void deleteKeyWithPrefix(String prefix) {
        try {
            //最后一定要带上 *
            Set<String> keys = stringRedisTemplate.keys(prefix + "*");
            stringRedisTemplate.delete(keys);
        } catch (Exception e) {
            log.error("Error occurred while deleting keys with prefix: {}", prefix, e);
        }
    }

    public <T> Page<T> getPageCache(String key, Class<T> clz) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            log.info("缓存存在，直接返回。");
            // 3.存在，直接返回
            Page<T> page = JSONUtil.toBean(json, new TypeReference<Page<T>>() {
            }, true);
            return page;
        }
        return null;
    }

    /**
     * 根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
     *
     * @param dbFallback 根据id查询数据库的方法
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            log.info("缓存存在，直接返回。");
            // 3.存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断命中的是否是空值
        if (json != null) {
            // 返回一个错误信息
            return null;
        }

        // 4.不存在，根据id查询数据库
        R r = dbFallback.apply(id);
        // 5.不存在，返回错误
        if (r == null) {
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", RedisConstant.CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }
        // 6.存在，写入redis
        this.set(key, r, time, unit);
        return r;
    }
}
