package com.dianping.ratelimiter.distributed.tokenBucket;

import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class DistributedTokenBucketRateLimiter {

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    public boolean tryAcquire(String key, int permitsPerSecond, int capacity, long currentTime, int permits){

        String script = getLuaScript();
        List<Long> res = redisTemplate.execute(new DefaultRedisScript<>(script, List.class),
                Collections.singletonList(key),
                permitsPerSecond, capacity, currentTime, permits);
        if (res != null && res.get(0) == 1){
            log.info("请求成功, 剩余令牌：{}",res.get(1));
            return true;
        }else {
            log.info("请求失败");
            return false;
        }
    }

    private String getLuaScript() {

        return "local key = KEYS[1]\n" +
                "local rate = tonumber(ARGV[1])\n" +
                "local capacity = tonumber(ARGV[2])\n" +
                "local current_time = tonumber(ARGV[3])\n" +
                "local requested = tonumber(ARGV[4])\n" +
                "local tokens_count_field = 'bucket_token_count'\n" +
                "local last_refreshed_field = 'last_update_time'\n" +
                "\n" +
                "local fill_time = capacity/rate\n" +
                "local ttl = math.floor(fill_time*2)\n" +
                "\n" +
                "-- 如果过期了，先让bucket充满令牌\n" +
                "local last_tokens = tonumber(redis.call(\"hget\", key, tokens_count_field))\n" +
                "if last_tokens == nil then\n" +
                "  last_tokens = capacity\n" +
                "end\n" +
                "\n" +
                "local last_refreshed = tonumber(redis.call(\"hget\", key, last_refreshed_field))\n" +
                "if last_refreshed == nil then\n" +
                "  last_refreshed = 0\n" +
                "end\n" +
                "\n" +
                "local delta = math.max(0, current_time - last_refreshed)\n" +
                "local filled_tokens = math.min(capacity, math.floor(last_tokens + (delta / 1000 * rate)))\n" +
                "local allowed = filled_tokens >= requested\n" +
                "local remain_tokens = filled_tokens\n" +
                "local allowed_num = 0\n" +
                "if allowed then\n" +
                "  remain_tokens = filled_tokens - requested\n" +
                "  allowed_num = 1\n" +
                "end\n" +
                "\n" +
                "redis.call(\"hset\", key, tokens_count_field, remain_tokens)\n" +
                "redis.call(\"hset\", key, last_refreshed_field, current_time)\n" +
                "redis.call(\"expire\", key, ttl)\n" +
                "\n" +
                "return { allowed_num, remain_tokens }";
    }
}
