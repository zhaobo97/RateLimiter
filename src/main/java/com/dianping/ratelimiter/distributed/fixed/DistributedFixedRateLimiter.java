package com.dianping.ratelimiter.distributed.fixed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;

@Slf4j
@Component
public class DistributedFixedRateLimiter {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 固定窗口限流
     * @param key 限流资源
     * @param windowSize 窗口大小
     * @param maxRequestCount 最大请求数
     * @return 请求是否通过
     */
    public boolean tryAcquire( String key,int windowSize, int maxRequestCount){
        String script = getFixedRateLimiterLuaScript();
        Long res = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(key),
                maxRequestCount, windowSize);
        if (res != null && res == -1) {
            log.info("请求失败");

            return true;
        }else {
            log.info("--- 请求成功 , 剩余可用请求数：{}", res);
            return false;
        }
    }

    private String getFixedRateLimiterLuaScript(){
        return "local key = KEYS[1] -- 限流资源\n" +
                "local limitCount = ARGV[1] -- 限流请求数\n" +
                "local limitTime = ARGV[2] -- 限流时间\n" +
                "local currentCount = redis.call('get', key) -- 当前请求数\n" +
                "if (currentCount and tonumber(currentCount) >= tonumber(limitCount)) then\n" +
                "    return -1\n" +
                "end\n" +
                "currentCount = redis.call('incr', key)\n" +
                "if (tonumber(currentCount) == 1) then\n" +
                "    redis.call('expire', key, limitTime)\n" +
                "end\n" +
                "return tonumber(limitCount) - tonumber(currentCount)";
    }

}
