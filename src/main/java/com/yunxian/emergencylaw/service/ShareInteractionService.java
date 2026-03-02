package com.yunxian.emergencylaw.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class ShareInteractionService {

    private static final TypeReference<Map<Long, Set<Long>>> TYPE = new TypeReference<Map<Long, Set<Long>>>() {};

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path filePath = Paths.get("data", "share_post_likes.json");

    private Map<Long, Set<Long>> cache;

    public synchronized boolean hasLiked(Long postId, Long userId) {
        if (postId == null || userId == null) return false;
        return load().getOrDefault(postId, new HashSet<>()).contains(userId);
    }

    public synchronized boolean addLike(Long postId, Long userId) {
        if (postId == null || userId == null) return false;
        Map<Long, Set<Long>> map = load();
        Set<Long> set = map.computeIfAbsent(postId, k -> new HashSet<>());
        if (set.contains(userId)) return false;
        set.add(userId);
        save(map);
        return true;
    }

    private Map<Long, Set<Long>> load() {
        if (cache != null) return cache;
        cache = new HashMap<>();
        try {
            if (!Files.exists(filePath)) return cache;
            byte[] bytes = Files.readAllBytes(filePath);
            if (bytes.length == 0) return cache;
            Map<Long, Set<Long>> map = objectMapper.readValue(bytes, TYPE);
            if (map != null) cache.putAll(map);
        } catch (Exception ignored) {
        }
        return cache;
    }

    private void save(Map<Long, Set<Long>> map) {
        try {
            Files.createDirectories(filePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), map);
            cache = new HashMap<>(map);
        } catch (Exception ignored) {
        }
    }
}
