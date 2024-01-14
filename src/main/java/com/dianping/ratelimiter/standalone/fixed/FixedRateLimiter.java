package com.dianping.ratelimiter.standalone.fixed;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class FixedRateLimiter {
    Logger logger = LoggerFactory.getLogger(FixedRateLimiter.class);
    long size;
    int maxCount;
    AtomicInteger counter = new AtomicInteger(0);
    long rightBorder; //窗口右边界
    public FixedRateLimiter(long windowSize, int maxRequestCount) {
        this.size = windowSize;
        this.maxCount = maxRequestCount;
        this.rightBorder = System.currentTimeMillis() + windowSize;
    }
    public synchronized boolean tryAcquire() {
        long currentTime = System.currentTimeMillis();
        if (rightBorder < currentTime) {
            while ((rightBorder += size) < currentTime){
                rightBorder += size;
            }
            counter = new AtomicInteger(0);
            logger.info("窗口重置");
        }

        if (counter.intValue() < maxCount) {
            counter.incrementAndGet();
            logger.info("请求成功");
            return true;
        } else {
            logger.info("请求失败");
            return false;
        }
    }
}