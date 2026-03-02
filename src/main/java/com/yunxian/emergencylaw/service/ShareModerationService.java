package com.yunxian.emergencylaw.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class ShareModerationService {

    private static final TypeReference<Map<Long, Boolean>> TYPE = new TypeReference<Map<Long, Boolean>>() {};

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path filePath = Paths.get("data", "share_post_moderation.json");

    private Map<Long, Boolean> cache;

    public synchronized boolean isHidden(Long postId) {
        if (postId == null) return false;
        return load().getOrDefault(postId, false);
    }

    public synchronized void hide(Long postId) {
        if (postId == null) return;
        Map<Long, Boolean> map = load();
        map.put(postId, true);
        save(map);
    }

    public synchronized void show(Long postId) {
        if (postId == null) return;
        Map<Long, Boolean> map = load();
        map.put(postId, false);
        save(map);
    }

    public synchronized Map<Long, Boolean> snapshot() {
        return new HashMap<>(load());
    }

    private Map<Long, Boolean> load() {
        if (cache != null) return cache;
        cache = new HashMap<>();
        try {
            if (!Files.exists(filePath)) return cache;
            byte[] bytes = Files.readAllBytes(filePath);
            if (bytes.length == 0) return cache;
            Map<Long, Boolean> map = objectMapper.readValue(bytes, TYPE);
            if (map != null) cache.putAll(map);
        } catch (Exception ignored) {
            // fallback to empty
        }
        return cache;
    }

    private void save(Map<Long, Boolean> map) {
        try {
            Files.createDirectories(filePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), map);
            cache = new HashMap<>(map);
        } catch (Exception ignored) {
            // ignore
        }
    }
}

