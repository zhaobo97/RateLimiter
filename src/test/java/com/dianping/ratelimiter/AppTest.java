package com.dianping.ratelimiter;

import com.dianping.ratelimiter.distributed.fixed.DistributedFixedRateLimiter;
import com.dianping.ratelimiter.distributed.slide.DistributedSlideWindowRateLimiter;
import com.dianping.ratelimiter.distributed.tokenBucket.DistributedTokenBucketRateLimiter;
import com.dianping.ratelimiter.standalone.fixed.FixedRateLimiter;
import com.dianping.ratelimiter.standalone.leakyBucket.LeakyBucketRateLimiter;
import com.dianping.ratelimiter.standalone.slide.SlidingWindowRateLimiter;
import com.dianping.ratelimiter.standalone.tokenBucket.GuavaRateLimiter;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Unit test for simple App.
 */
@Slf4j
@SpringBootTest
public class AppTest {

    public void test01() {
        FixedRateLimiter limiter = new FixedRateLimiter(2, 5);
        for (int i = 0; i < 10; i++) {
            boolean b = limiter.tryAcquire();
        }
    }

    public void test02() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(5, 5, 10);
        for (int i = 0; i < 20; i++) {
            boolean b = limiter.tryAcquire();
        }
    }

    public void test03() throws InterruptedException {
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(10, 2);
        for (int i = 0; i < 20; i++) {
            boolean b = limiter.tryAcquire();
            if (!b) {
                Thread.sleep(1000);
            }
        }
    }

    public void test04() {
        GuavaRateLimiter limiter = new GuavaRateLimiter(10);
        long startTimeStamp = System.currentTimeMillis();
        for (int i = 0; i < 15; i++) {
            double time = limiter.acquire();
            log.info("等待时间:{}s, 总时间:{}ms", time, System.currentTimeMillis() - startTimeStamp);
        }
    }

    @Resource
    private DistributedFixedRateLimiter fixedRateLimiter;

    // 固定窗口
    @Test
    public void test05() {
        String key = "/fixed/window";
        for (int i = 0; i < 20; i++) {
            boolean b = fixedRateLimiter.tryAcquire(key,5, 5);
        }
    }

    @Resource
    private DistributedSlideWindowRateLimiter slidingWindowRateLimiter;
    // 滑动窗口
    @Test
    public void test06() throws Exception {
        String key = "/slide/window";
        for (int i = 0; i < 20; i++) {
            long currentTime = System.currentTimeMillis();
            boolean b = slidingWindowRateLimiter.tryAcquire(key, "request-"+i, 5, 5, currentTime);
            Thread.sleep(1000);
            if (b){
                log.info("请求成功");
            }else {
                log.info("请求失败");
            }
        }
    }

    @Resource
    private DistributedTokenBucketRateLimiter tokenBucketRateLimiter;

    @Test
    public void test07() throws InterruptedException {
        String key = "/token/bucket";
        for (int i = 0; i < 20; i++) {
            long currentTime = System.currentTimeMillis();
            Thread.sleep(500);
            boolean b = tokenBucketRateLimiter.tryAcquire(key, 1, 10, currentTime, 3);
        }
    }
}
