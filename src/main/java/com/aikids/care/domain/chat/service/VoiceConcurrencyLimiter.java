package com.aikids.care.domain.chat.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

/**
 * 음성 상담(STT + LLM + 음성 합성)은 메모리·CPU 부담이 큰 동시성 한계 자원이다.
 * 시연·발표 같이 트래픽이 몰릴 때 동시에 너무 많은 음성 요청이 들어오면 OOM 또는
 * CPU 100% 로 전체 서비스가 마비될 수 있어, 별도 Semaphore 로 동시 진행 수를 제한한다.
 * <p>
 * 한도는 {@code app.voice.max-concurrent} (default 2) 로 외부화. t3.small (2 vCPU/2GB)
 * 기준 보수적으로 2 로 설정했고, 더 큰 인스턴스에서는 yml 로 늘릴 수 있다.
 */
@Slf4j
@Component
public class VoiceConcurrencyLimiter {

    @Value("${app.voice.max-concurrent:2}")
    private int maxConcurrent;

    private Semaphore semaphore;

    @PostConstruct
    void init() {
        this.semaphore = new Semaphore(maxConcurrent, true);
        log.info("VoiceConcurrencyLimiter initialized: maxConcurrent={}", maxConcurrent);
    }

    /**
     * 즉시 슬롯 확보 시도. 대기하지 않고 슬롯이 없으면 false 를 반환한다.
     */
    public boolean tryAcquire() {
        boolean acquired = semaphore.tryAcquire();
        if (!acquired) {
            log.warn("Voice slot unavailable: availablePermits={}, queueLength={}",
                    semaphore.availablePermits(), semaphore.getQueueLength());
        }
        return acquired;
    }

    public void release() {
        semaphore.release();
    }

    public int availableSlots() {
        return semaphore.availablePermits();
    }
}
