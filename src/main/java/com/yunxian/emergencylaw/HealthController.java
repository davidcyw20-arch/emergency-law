package com.yunxian.emergencylaw;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public String health() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            return "OK ✅ DB Connected: " + conn.getMetaData().getURL();
        }
    }
}
