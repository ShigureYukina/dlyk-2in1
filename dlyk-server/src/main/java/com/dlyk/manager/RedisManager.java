package com.dlyk.manager;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/** copy by ShigureYukina,from 2025/8/21-下午1:51 */
@Component
public class RedisManager {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void setValue(String key, Object data) {
        redisTemplate.opsForValue().set(key, data);
    }
}