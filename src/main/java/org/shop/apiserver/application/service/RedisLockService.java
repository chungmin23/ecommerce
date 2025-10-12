package org.shop.apiserver.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Log4j2
public class RedisLockService {

    private final RedissonClient redissonClient;

    /**
     * Redis 분산락을 사용하여 작업 실행
     */
    public <T> T executeWithLock(String lockKey, int waitTime, int leaseTime, Supplier<T> supplier) {

        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);

            if (!acquired) {
                log.error("Lock 획득 실패: {}", lockKey);
                throw new IllegalStateException("현재 다른 사용자가 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            log.info("Lock 획득 성공: {}", lockKey);
            return supplier.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock 획득 중 인터럽트 발생", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Lock 해제: {}", lockKey);
            }
        }
    }

    public void executeWithLock(String lockKey, int waitTime, int leaseTime, Runnable runnable) {
        executeWithLock(lockKey, waitTime, leaseTime, () -> {
            runnable.run();
            return null;
        });
    }
}
