package com.yunxian.emergencylaw.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class ShareTagService {

    private static final TypeReference<Map<Long, List<String>>> TYPE = new TypeReference<Map<Long, List<String>>>() {};

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path filePath = Paths.get("data", "share_post_tags.json");

    private Map<Long, List<String>> cache;

    public synchronized List<String> getTags(Long postId) {
        if (postId == null) return new ArrayList<>();
        return new ArrayList<>(load().getOrDefault(postId, new ArrayList<>()));
    }

    public synchronized void setTags(Long postId, List<String> tags) {
        if (postId == null) return;
        Map<Long, List<String>> map = load();
        map.put(postId, normalize(tags));
        save(map);
    }

    private List<String> normalize(List<String> tags) {
        if (tags == null) return new ArrayList<>();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String t : tags) {
            if (t == null) continue;
            String v = t.trim();
            if (!v.isEmpty()) set.add(v);
        }
        return new ArrayList<>(set);
    }

    private Map<Long, List<String>> load() {
        if (cache != null) return cache;
        cache = new HashMap<>();
        try {
            if (!Files.exists(filePath)) return cache;
            byte[] bytes = Files.readAllBytes(filePath);
            if (bytes.length == 0) return cache;
            Map<Long, List<String>> map = objectMapper.readValue(bytes, TYPE);
            if (map != null) cache.putAll(map);
        } catch (Exception ignored) {
        }
        return cache;
    }

    private void save(Map<Long, List<String>> map) {
        try {
            Files.createDirectories(filePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), map);
            cache = new HashMap<>(map);
        } catch (Exception ignored) {
        }
    }
}
