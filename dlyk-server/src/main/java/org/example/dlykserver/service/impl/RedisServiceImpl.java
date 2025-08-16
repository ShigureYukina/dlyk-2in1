package org.example.dlykserver.service.impl;


import jakarta.annotation.Resource;
import org.example.dlykserver.manager.RedisManager;
import org.example.dlykserver.service.RedisService;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisServiceImpl implements RedisService {

    @Resource
    private RedisManager redisManager;

    @Override
    public void setValue(String key, Object value) {
        redisManager.setValue(key, value);
    }

    @Override
    public Object getValue(String key) {
        return redisManager.getValue(key);
    }

    @Override
    public Boolean delete(String key) {
        return redisManager.delete(key);
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisManager.expire(key, timeout, unit);
    }
}
