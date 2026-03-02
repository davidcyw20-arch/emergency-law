package com.yunxian.emergencylaw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_lesson")
public class CourseLesson {
    private Long id;
    private Long courseId;
    private String title;
    private String contentType;
    private String contentText;
    private String contentUrl;
    private Integer durationMin;
    private Integer sortNo;
    private Integer enabled;
    private LocalDateTime createTime;
}
