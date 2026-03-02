package com.yunxian.emergencylaw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("study_progress")
public class StudyProgress {
    private Long id;
    private Long userId;
    private Long courseId;
    private Long lessonId;
    private String status;          // LEARNING / DONE
    private Integer progressPercent; // 0~100
    private LocalDateTime lastStudyTime;
}
