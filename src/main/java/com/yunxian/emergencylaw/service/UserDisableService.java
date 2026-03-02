package com.yunxian.emergencylaw.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@Service
public class UserDisableService {

    private static final TypeReference<Set<Long>> TYPE = new TypeReference<Set<Long>>() {};

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path filePath = Paths.get("data", "disabled_users.json");

    private Set<Long> cache;

    public synchronized boolean isDisabled(Long userId) {
        if (userId == null) return false;
        return load().contains(userId);
    }

    public synchronized void disable(Long userId) {
        if (userId == null) return;
        Set<Long> set = load();
        set.add(userId);
        save(set);
    }

    public synchronized void enable(Long userId) {
        if (userId == null) return;
        Set<Long> set = load();
        set.remove(userId);
        save(set);
    }

    public synchronized Set<Long> snapshot() {
        return new HashSet<>(load());
    }

    private Set<Long> load() {
        if (cache != null) return cache;
        cache = new HashSet<>();
        try {
            if (!Files.exists(filePath)) return cache;
            byte[] bytes = Files.readAllBytes(filePath);
            if (bytes.length == 0) return cache;
            Set<Long> set = objectMapper.readValue(bytes, TYPE);
            if (set != null) cache.addAll(set);
        } catch (Exception ignored) {
            // fallback to empty
        }
        return cache;
    }

    private void save(Set<Long> set) {
        try {
            Files.createDirectories(filePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), set);
            cache = new HashSet<>(set);
        } catch (Exception ignored) {
            // ignore
        }
    }
}

