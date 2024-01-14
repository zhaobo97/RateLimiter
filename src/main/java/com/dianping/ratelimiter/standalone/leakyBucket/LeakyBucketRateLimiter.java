package com.dianping.ratelimiter.standalone.leakyBucket;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LeakyBucketRateLimiter {
    Logger logger = LoggerFactory.getLogger(LeakyBucketRateLimiter.class);
    int capacity;
    AtomicInteger waterLevel = new AtomicInteger(); // 当前水位
    long startTimestamp;
    int leakRate;

    public LeakyBucketRateLimiter(int capacity, int leakRate) {
        this.capacity = capacity;
        this.leakRate = leakRate;
    }

    public synchronized boolean tryAcquire() {
        //桶中没有水， 重新开始计算
        if (waterLevel.get() == 0) {
            logger.info("开始漏水");
            startTimestamp = System.currentTimeMillis();
            waterLevel.incrementAndGet();
            return waterLevel.get() < capacity;
        }
        //先漏水，计算剩余水量
        long currentTime = System.currentTimeMillis();
        int leakedWater = (int) ((currentTime - startTimestamp) / 1000 * leakRate);
        logger.info("开始放行时间:{}, 当前时间:{}. 放行流量:{}", startTimestamp, currentTime, leakedWater);
        if (leakedWater != 0) {
            // 重新计算水位
            int leftWater = waterLevel.get() - leakedWater;
            waterLevel.set(leftWater > 0 ? leakedWater : 0);
            // 重置开始时间戳
            startTimestamp = System.currentTimeMillis();
        }
        logger.info("剩余容量:{}", capacity - waterLevel.get());
        if (waterLevel.get() < capacity) {
            logger.info("请求成功");
            waterLevel.incrementAndGet();
            return true;
        } else {
            logger.info("请求失败");
            return false;
        }
    }
}