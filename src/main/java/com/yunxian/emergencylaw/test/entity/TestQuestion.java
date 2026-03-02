package com.yunxian.emergencylaw.test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_question")
public class TestQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paperId;

    private String stem;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    private Integer answerIndex; // 0/1/2/3
    private Integer sortNo;

    private LocalDateTime createdAt;
}
