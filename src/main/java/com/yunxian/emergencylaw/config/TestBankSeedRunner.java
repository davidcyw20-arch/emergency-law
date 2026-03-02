package com.yunxian.emergencylaw.config;

import com.yunxian.emergencylaw.test.service.TestBankSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestBankSeedRunner implements CommandLineRunner {

    private final TestBankSeedService testBankSeedService;

    @Value("${app.seed.testbank:true}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled) return;
        try {
            testBankSeedService.seed(false);
        } catch (Exception ignored) {
            // ignore
        }
    }
}

