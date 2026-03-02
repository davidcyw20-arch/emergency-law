package com.yunxian.emergencylaw.test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_paper")
public class TestPaper {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String difficulty; // EASY/MEDIUM/HARD
    private Integer durationMin;
    private String tags;       // "基础,常识"
    private Integer enabled;   // 1/0

    private LocalDateTime createdAt;
}
