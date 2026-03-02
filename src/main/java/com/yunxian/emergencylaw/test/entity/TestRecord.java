package com.yunxian.emergencylaw.test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_record")
public class TestRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long paperId;

    private Integer score;
    private Integer correctCount;
    private Integer total;
    private Integer usedMin;

    private String answersJson;

    private LocalDateTime createdAt;
}
