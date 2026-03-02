package com.yunxian.emergencylaw.test.dto;

import lombok.Data;

import java.util.List;

@Data
public class TestSubmitRequest {
    private Long paperId;
    private List<Integer> answers; // 与题目顺序一致，单选：0/1/2/3
    private List<Long> questionIds; // 与 answers 顺序一致（解决乱序/自动组卷导致的评分不一致）
    private Integer usedMin;       // 前端可传，不传则后端按1分钟
}
