package com.yunxian.emergencylaw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {
    private Long id;
    private Long categoryId;
    private String title;
    private String coverUrl;
    private String summary;
    private String difficulty;
    private Integer enabled;
    private Integer sortNo;
    private LocalDateTime createTime;
}
