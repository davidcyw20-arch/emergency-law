package com.yunxian.emergencylaw;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// ✅ 扫描整个项目下所有 mapper（包含 com.yunxian.emergencylaw.mapper 和 com.yunxian.emergencylaw.test.mapper）
@MapperScan("com.yunxian.emergencylaw")
public class EmergencyLawApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmergencyLawApplication.class, args);
    }
}
