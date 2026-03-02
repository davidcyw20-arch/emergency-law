package com.yunxian.emergencylaw.test.vo;

import lombok.Data;

import java.util.List;

@Data
public class TestPaperListVO {
    private Long id;
    private String title;
    private String difficulty;
    private Integer durationMin;
    private Integer questionCount;
    private List<String> tags;
}
