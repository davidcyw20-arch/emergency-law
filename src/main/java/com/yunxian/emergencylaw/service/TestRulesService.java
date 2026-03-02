package com.yunxian.emergencylaw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class TestRulesService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path filePath = Paths.get("data", "test_rules.json");

    private TestRules cache;

    public synchronized TestRules getRules() {
        if (cache != null) return sanitize(cache);
        cache = load();
        return sanitize(cache);
    }

    public synchronized TestRules saveRules(TestRules rules) {
        TestRules next = sanitize(rules);
        try {
            Files.createDirectories(filePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), next);
        } catch (Exception ignored) {
            // ignore
        }
        cache = next;
        return next;
    }

    private TestRules load() {
        try {
            if (!Files.exists(filePath)) return defaults();
            byte[] bytes = Files.readAllBytes(filePath);
            if (bytes.length == 0) return defaults();
            TestRules rules = objectMapper.readValue(bytes, TestRules.class);
            return rules == null ? defaults() : rules;
        } catch (Exception ignored) {
            return defaults();
        }
    }

    private TestRules sanitize(TestRules in) {
        TestRules d = defaults();
        if (in == null) return d;

        TestRules out = new TestRules();
        out.setAutoAssemble(in.getAutoAssemble() != null ? in.getAutoAssemble() : d.getAutoAssemble());
        out.setShuffle(in.getShuffle() != null ? in.getShuffle() : d.getShuffle());
        out.setAllowRetake(in.getAllowRetake() != null ? in.getAllowRetake() : d.getAllowRetake());

        Integer passScore = in.getPassScore() != null ? in.getPassScore() : d.getPassScore();
        if (passScore < 0) passScore = 0;
        if (passScore > 100) passScore = 100;
        out.setPassScore(passScore);

        Integer timeLimit = in.getTimeLimit() != null ? in.getTimeLimit() : d.getTimeLimit();
        if (timeLimit < 1) timeLimit = 1;
        if (timeLimit > 600) timeLimit = 600;
        out.setTimeLimit(timeLimit);

        Integer attempts = in.getAttempts() != null ? in.getAttempts() : d.getAttempts();
        if (attempts < 1) attempts = 1;
        if (attempts > 20) attempts = 20;
        out.setAttempts(attempts);

        Integer questionCount = in.getQuestionCount() != null ? in.getQuestionCount() : d.getQuestionCount();
        if (questionCount < 1) questionCount = 1;
        if (questionCount > 200) questionCount = 200;
        out.setQuestionCount(questionCount);

        Integer easyPct = in.getEasyPct() != null ? in.getEasyPct() : d.getEasyPct();
        Integer mediumPct = in.getMediumPct() != null ? in.getMediumPct() : d.getMediumPct();
        Integer hardPct = in.getHardPct() != null ? in.getHardPct() : d.getHardPct();

        if (easyPct < 0) easyPct = 0;
        if (mediumPct < 0) mediumPct = 0;
        if (hardPct < 0) hardPct = 0;

        int sum = easyPct + mediumPct + hardPct;
        if (sum <= 0) {
            easyPct = d.getEasyPct();
            mediumPct = d.getMediumPct();
            hardPct = d.getHardPct();
            sum = easyPct + mediumPct + hardPct;
        }
        if (sum != 100) {
            double scale = 100.0 / sum;
            int ne = (int) Math.round(easyPct * scale);
            int nm = (int) Math.round(mediumPct * scale);
            int nh = 100 - ne - nm;
            easyPct = Math.max(0, Math.min(100, ne));
            mediumPct = Math.max(0, Math.min(100, nm));
            hardPct = Math.max(0, Math.min(100, nh));
        }
        out.setEasyPct(easyPct);
        out.setMediumPct(mediumPct);
        out.setHardPct(hardPct);

        return out;
    }

    private TestRules defaults() {
        TestRules rules = new TestRules();
        rules.setAutoAssemble(true);
        rules.setShuffle(true);
        rules.setAllowRetake(true);
        rules.setPassScore(80);
        rules.setTimeLimit(60);
        rules.setAttempts(3);
        rules.setQuestionCount(10);
        rules.setEasyPct(45);
        rules.setMediumPct(40);
        rules.setHardPct(15);
        return rules;
    }

    @Data
    public static class TestRules {
        private Boolean autoAssemble;
        private Boolean shuffle;
        private Boolean allowRetake;
        private Integer passScore;
        private Integer timeLimit;
        private Integer attempts;
        private Integer questionCount;
        private Integer easyPct;
        private Integer mediumPct;
        private Integer hardPct;
    }
}
