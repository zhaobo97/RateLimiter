package com.dianping.ratelimiter.standalone.slide;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class SlidingWindowRateLimiter {
    Logger logger = LoggerFactory.getLogger(SlidingWindowRateLimiter.class);
    long size;
    int shardNum; //分片窗口数
    int maxPermits; //允许通过的最大请求数
    int[] shardCount;   //各个窗口内请求数
    int totalCount; //当前请求总数
    int shardId; //当前窗口下标
    long subWindowSize; //每个子窗口大小，毫秒
    //窗口右边界
    long rightBorder;

    public SlidingWindowRateLimiter(long windowSize, int shardNum, int maxRequestCount) {
        this.size = windowSize;
        this.shardNum = shardNum;
        this.maxPermits = maxRequestCount;
        this.shardCount = new int[shardNum];
        this.subWindowSize = windowSize / shardNum;
        this.rightBorder = System.currentTimeMillis();
    }
    public synchronized boolean tryAcquire() {
        long currentTime = System.currentTimeMillis();
        if (rightBorder < currentTime) {
            do {
                // 为新的子窗口分配id
                shardId = (++shardId) % shardNum;
                // 释放过期的 permits
                totalCount -= shardCount[shardId];
                // 为新的子窗口初始化计数器
                shardCount[shardId] = 0;
                // 移动有边界
                rightBorder += subWindowSize;
                logger.info("窗口重置");
            } while (rightBorder < currentTime);
        }

        if (totalCount < maxPermits) {
            logger.info("请求成功:{}", shardId);
            shardCount[shardId]++;
            totalCount++;
            return true;
        } else {
            logger.info("请求失败");
            return false;
        }
    }
}