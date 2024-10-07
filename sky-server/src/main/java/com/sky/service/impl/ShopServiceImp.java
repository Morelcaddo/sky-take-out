package com.sky.service.impl;

import com.sky.constant.ShopConstant;
import com.sky.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImp implements ShopService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Integer getStatus() {
        return (Integer) redisTemplate.opsForValue().get(ShopConstant.STATUS_KEY);
    }

    @Override
    public void setStatus(Integer status) {
        redisTemplate.opsForValue().set(ShopConstant.STATUS_KEY, status);
    }
}
