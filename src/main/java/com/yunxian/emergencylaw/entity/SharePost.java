package com.yunxian.emergencylaw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("share_post")
public class SharePost {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String region;
    private Integer likes;
    private LocalDateTime createdAt;
}
