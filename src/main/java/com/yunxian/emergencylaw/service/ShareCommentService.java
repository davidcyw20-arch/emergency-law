package com.yunxian.emergencylaw.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ShareCommentService {

    private static final TypeReference<Map<Long, List<CommentRecord>>> TYPE = new TypeReference<Map<Long, List<CommentRecord>>>() {};

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path filePath = Paths.get("data", "share_post_comments.json");

    private Map<Long, List<CommentRecord>> cache;

    public synchronized List<CommentRecord> list(Long postId) {
        if (postId == null) return new ArrayList<>();
        return new ArrayList<>(load().getOrDefault(postId, new ArrayList<>()));
    }

    public synchronized int count(Long postId) {
        return list(postId).size();
    }

    public synchronized CommentRecord add(Long postId, Long userId, String author, String content) {
        Map<Long, List<CommentRecord>> map = load();
        List<CommentRecord> list = map.computeIfAbsent(postId, k -> new ArrayList<>());

        CommentRecord item = new CommentRecord();
        item.setId(System.currentTimeMillis());
        item.setPostId(postId);
        item.setUserId(userId);
        item.setAuthor(author);
        item.setContent(content == null ? "" : content.trim());
        item.setCreatedAt(LocalDateTime.now());

        list.add(item);
        save(map);
        return item;
    }

    private Map<Long, List<CommentRecord>> load() {
        if (cache != null) return cache;
        cache = new HashMap<>();
        try {
            if (!Files.exists(filePath)) return cache;
            byte[] bytes = Files.readAllBytes(filePath);
            if (bytes.length == 0) return cache;
            Map<Long, List<CommentRecord>> map = objectMapper.readValue(bytes, TYPE);
            if (map != null) cache.putAll(map);
        } catch (Exception ignored) {
        }
        return cache;
    }

    private void save(Map<Long, List<CommentRecord>> map) {
        try {
            Files.createDirectories(filePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), map);
            cache = new HashMap<>(map);
        } catch (Exception ignored) {
        }
    }

    @Data
    public static class CommentRecord {
        private Long id;
        private Long postId;
        private Long userId;
        private String author;
        private String content;
        private LocalDateTime createdAt;
    }
}
