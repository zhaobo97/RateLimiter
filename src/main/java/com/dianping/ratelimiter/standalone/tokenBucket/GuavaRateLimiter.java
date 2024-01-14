package com.dianping.ratelimiter.standalone.tokenBucket;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class GuavaRateLimiter {

    private RateLimiter rateLimiter;

    public GuavaRateLimiter(double permitsPerSecond) {
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
    }
    public GuavaRateLimiter(double permitsPerSecond, long warmUpPeriodAsSecond, TimeUnit timeUnit) {
        this.rateLimiter = RateLimiter.create(permitsPerSecond, warmUpPeriodAsSecond, timeUnit);
    }

    public boolean tryAcquire(int permits){
        return rateLimiter.tryAcquire(permits);
    }

    public boolean tryAcquire(int permits, long warmUpPeriodAsSecond, TimeUnit timeUnit){
        return rateLimiter.tryAcquire(permits, warmUpPeriodAsSecond, timeUnit);
    }

    public double acquire() {
        return rateLimiter.acquire();
    }
}
