package it.luncent.cloud_storage.common.config;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Configuration
public class ForkJoinPoolConfig {

    @Bean
    public ForkJoinPool forkJoinPool() {
        return ForkJoinPool.commonPool();
    }

    @Bean
    public ForkJoinPoolShutdown forkJoinPoolShutdown() {
        return new ForkJoinPoolShutdown(forkJoinPool());
    }

    @RequiredArgsConstructor
    @Slf4j
    public static class ForkJoinPoolShutdown {

        private final ForkJoinPool forkJoinPool;

        @PreDestroy
        public void shutdownForkJoinPool() {
            try {
                boolean terminated = forkJoinPool.awaitQuiescence(30, TimeUnit.SECONDS);
                if (terminated) {
                    log.info("ForkJoinPool gracefully terminated");
                } else {
                    log.info("ForkJoinPool termination timeout");
                }
            } catch (Exception e) {
                log.error("Error during ForkJoinPool shutdown: {}", e.getMessage(), e);
            }
        }
    }
}
