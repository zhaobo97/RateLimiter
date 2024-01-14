package com.dianping.ratelimiter.distributed.slide;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Component
public class DistributedSlideWindowRateLimiter {

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    public boolean tryAcquire(String key, String currentTimeKey ,long windowSize, int limitCount, long currentTime) throws Exception {
        String script = getSlideWindowLuaScript();
        List<Long> result = redisTemplate.execute(new DefaultRedisScript<>(script, List.class),
                Arrays.asList(new String[]{key, currentTimeKey}),
                windowSize, limitCount, currentTime);
        if (result == null)
            throw new Exception("redis execute occur exception!");
        log.info("剩余请求数：{}", result.get(1));
        return result.get(0) == 1;
    }

    private String getSlideWindowLuaScript() {
        return "local key = KEYS[1]  -- 限流关键字\n" +
                "local current_time_key = KEYS[2]   -- 当前时间戳的key\n"+
                "local window_size = tonumber(ARGV[1])  -- 滑动窗口大小\n" +
                "local limit = tonumber(ARGV[2])  -- 限制的请求数\n" +
                "local current_time = tonumber(ARGV[3])  -- 当前时间戳\n" +
                "local last_requested = 0   -- 已经用掉的请求数\n"+
                "local remain_request = 0   -- 剩余可以分配的请求数\n"+
                "local allowed_num = 0  -- 本次允许通过的请求数\n" +
                "\n" +
                "local exists_key = redis.call('exists', key)\n" +
                "if (exists_key == 1) then\n" +
                "    last_requested = redis.call('zcard', key)\n" +
                "end\n"+
                "remain_request = limit - last_requested\n" +
                "if (last_requested < limit) then\n" +
                "    allowed_num = 1\n" +
                "    redis.call('zadd', key, current_time, current_time_key)\n" +
                "end\n" +
                "redis.call('zremrangebyscore', key, 0, current_time - window_size * 1000)\n" +
                "redis.call('expire', key, window_size)\n" +
                "\n" +
                "return { allowed_num, remain_request }";
    }
}
