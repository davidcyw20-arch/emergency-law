package com.yunxian.emergencylaw.test.vo;

import lombok.Data;

import java.util.List;

@Data
public class TestPaperDetailVO {
    private Long id;
    private String title;
    private String difficulty;
    private Integer durationMin;
    private List<String> tags;
    private Integer questionCount;
    private List<QuestionVO> questions;

    @Data
    public static class QuestionVO {
        private Long id;
        private String type; // SINGLE
        private String stem;
        private List<String> options;
        private Integer answer; // 注意：给前端做题用的“正确答案”，正式系统可以不下发；你答辩演示先下发没关系
    }
}
