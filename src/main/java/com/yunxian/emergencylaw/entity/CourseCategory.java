package com.yunxian.emergencylaw.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_category")
public class CourseCategory {
    private Long id;
    private String name;
    private Integer sortNo;
    private Integer enabled;
    private LocalDateTime createTime;
}
