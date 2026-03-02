package com.yunxian.emergencylaw.test.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunxian.emergencylaw.test.dto.TestSubmitRequest;
import com.yunxian.emergencylaw.test.entity.TestPaper;
import com.yunxian.emergencylaw.test.entity.TestQuestion;
import com.yunxian.emergencylaw.test.entity.TestRecord;
import com.yunxian.emergencylaw.test.mapper.TestPaperMapper;
import com.yunxian.emergencylaw.test.mapper.TestQuestionMapper;
import com.yunxian.emergencylaw.test.mapper.TestRecordMapper;
import com.yunxian.emergencylaw.test.vo.TestPaperDetailVO;
import com.yunxian.emergencylaw.test.vo.TestPaperListVO;
import com.yunxian.emergencylaw.service.TestRulesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestPaperMapper paperMapper;
    private final TestQuestionMapper questionMapper;
    private final TestRecordMapper recordMapper;
    private final TestRulesService testRulesService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<TestPaperListVO> listPapers() {
        TestRulesService.TestRules rules = testRulesService.getRules();

        List<TestPaper> papers = paperMapper.selectList(
                new LambdaQueryWrapper<TestPaper>()
                        .eq(TestPaper::getEnabled, 1)
                        .orderByDesc(TestPaper::getId)
        );

        if (papers.isEmpty()) return new ArrayList<>();

        List<Long> paperIds = papers.stream().map(TestPaper::getId).collect(Collectors.toList());
        Map<Long, Long> countMap = questionMapper.selectList(
                new LambdaQueryWrapper<TestQuestion>().in(TestQuestion::getPaperId, paperIds)
        ).stream().collect(Collectors.groupingBy(TestQuestion::getPaperId, Collectors.counting()));

        List<TestPaperListVO> res = new ArrayList<>();
        for (TestPaper p : papers) {
            TestPaperListVO vo = new TestPaperListVO();
            vo.setId(p.getId());
            vo.setTitle(p.getTitle());
            vo.setDifficulty(p.getDifficulty());
            vo.setDurationMin(p.getDurationMin());
            vo.setQuestionCount(Math.toIntExact(countMap.getOrDefault(p.getId(), 0L)));
            vo.setTags(splitTags(p.getTags()));

            if (rules != null && Boolean.TRUE.equals(rules.getAutoAssemble())) {
                if (rules.getQuestionCount() != null) vo.setQuestionCount(rules.getQuestionCount());
                if (rules.getTimeLimit() != null) vo.setDurationMin(rules.getTimeLimit());
            }
            res.add(vo);
        }
        return res;
    }

    public TestPaperDetailVO paperDetail(Long paperId) {
        TestPaper paper = paperMapper.selectById(paperId);
        if (paper == null || paper.getEnabled() == null || paper.getEnabled() != 1) return null;

        TestRulesService.TestRules rules = testRulesService.getRules();

        List<TestQuestion> qs;
        if (rules != null && Boolean.TRUE.equals(rules.getAutoAssemble())) {
            qs = assembleQuestions(paper, rules);
        } else {
            qs = questionMapper.selectList(
                    new LambdaQueryWrapper<TestQuestion>()
                            .eq(TestQuestion::getPaperId, paperId)
                            .orderByAsc(TestQuestion::getSortNo)
                            .orderByAsc(TestQuestion::getId)
            );
        }

        if (rules != null && Boolean.TRUE.equals(rules.getShuffle())) {
            Collections.shuffle(qs);
        }

        TestPaperDetailVO vo = new TestPaperDetailVO();
        vo.setId(paper.getId());
        vo.setTitle(paper.getTitle());
        vo.setDifficulty(paper.getDifficulty());
        vo.setDurationMin(rules != null && rules.getTimeLimit() != null ? rules.getTimeLimit() : paper.getDurationMin());
        vo.setTags(splitTags(paper.getTags()));
        vo.setQuestionCount(qs.size());

        List<TestPaperDetailVO.QuestionVO> qvos = new ArrayList<>();
        for (TestQuestion q : qs) {
            TestPaperDetailVO.QuestionVO qvo = new TestPaperDetailVO.QuestionVO();
            qvo.setId(q.getId());
            qvo.setType("SINGLE");
            qvo.setStem(q.getStem());
            qvo.setOptions(Arrays.asList(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()));
            qvo.setAnswer(q.getAnswerIndex());
            qvos.add(qvo);
        }
        vo.setQuestions(qvos);
        return vo;
    }

    public Map<String, Object> submit(Long userId, TestSubmitRequest req) {
        Long paperId = req.getPaperId();
        if (paperId == null) throw new RuntimeException("paperId is required");

        TestRulesService.TestRules rules = testRulesService.getRules();

        TestPaper paper = paperMapper.selectById(paperId);
        if (paper == null || paper.getEnabled() == null || paper.getEnabled() != 1) throw new RuntimeException("paper not found");

        if (rules != null) {
            Long attemptCount = recordMapper.selectCount(
                    new LambdaQueryWrapper<TestRecord>()
                            .eq(TestRecord::getUserId, userId)
                            .eq(TestRecord::getPaperId, paperId)
            );

            boolean allowRetake = rules.getAllowRetake() == null || rules.getAllowRetake();
            int maxAttempts = rules.getAttempts() == null ? 1 : rules.getAttempts();
            if (!allowRetake && attemptCount != null && attemptCount > 0) {
                throw new RuntimeException("不允许重复答题");
            }
            if (allowRetake && maxAttempts > 0 && attemptCount != null && attemptCount >= maxAttempts) {
                throw new RuntimeException("已达到最大答题次数限制：" + maxAttempts);
            }

            Integer timeLimit = rules.getTimeLimit();
            Integer usedMin = req.getUsedMin();
            if (timeLimit != null && timeLimit > 0 && usedMin != null && usedMin > timeLimit) {
                throw new RuntimeException("已超出限时：" + timeLimit + " 分钟");
            }
        }

        List<Integer> answers = req.getAnswers();
        if (answers == null) answers = new ArrayList<>();

        List<TestQuestion> qs = resolveSubmitQuestions(paperId, req.getQuestionIds(), rules);
        int total = qs.size();
        int correct = 0;

        for (int i = 0; i < total; i++) {
            Integer a = (i < answers.size() ? answers.get(i) : null);
            if (a != null && a < 0) a = null;
            Integer right = qs.get(i).getAnswerIndex();
            if (a != null && right != null && a.intValue() == right.intValue()) correct++;
        }

        int score = total == 0 ? 0 : (int) Math.round((correct * 100.0) / total);
        int usedMin = (req.getUsedMin() == null || req.getUsedMin() <= 0) ? 1 : req.getUsedMin();

        TestRecord r = new TestRecord();
        r.setUserId(userId);
        r.setPaperId(paperId);
        r.setScore(score);
        r.setCorrectCount(correct);
        r.setTotal(total);
        r.setUsedMin(usedMin);
        r.setCreatedAt(LocalDateTime.now());
        try {
            r.setAnswersJson(objectMapper.writeValueAsString(answers));
        } catch (Exception e) {
            r.setAnswersJson("[]");
        }
        recordMapper.insert(r);

        Map<String, Object> out = new HashMap<>();
        out.put("score", score);
        out.put("correctCount", correct);
        out.put("total", total);
        out.put("usedMin", usedMin);
        out.put("recordId", r.getId());
        return out;
    }

    public List<TestRecord> myHistory(Long userId) {
        return recordMapper.selectList(
                new LambdaQueryWrapper<TestRecord>()
                        .eq(TestRecord::getUserId, userId)
                        .orderByDesc(TestRecord::getId)
        );
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.trim().isEmpty()) return new ArrayList<>();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<TestQuestion> resolveSubmitQuestions(Long paperId, List<Long> questionIds, TestRulesService.TestRules rules) {
        boolean requireIds = rules != null && (Boolean.TRUE.equals(rules.getAutoAssemble()) || Boolean.TRUE.equals(rules.getShuffle()));
        if (questionIds == null || questionIds.isEmpty()) {
            if (requireIds) throw new RuntimeException("试卷已启用自动组卷/乱序，请刷新试卷后再提交");
            return questionMapper.selectList(
                    new LambdaQueryWrapper<TestQuestion>()
                            .eq(TestQuestion::getPaperId, paperId)
                            .orderByAsc(TestQuestion::getSortNo)
                            .orderByAsc(TestQuestion::getId)
            );
        }

        List<TestQuestion> raw = questionMapper.selectList(
                new LambdaQueryWrapper<TestQuestion>().in(TestQuestion::getId, questionIds)
        );
        if (raw == null || raw.isEmpty()) throw new RuntimeException("题目不存在");

        Map<Long, TestQuestion> map = raw.stream().collect(Collectors.toMap(TestQuestion::getId, q -> q, (a, b) -> a));
        List<TestQuestion> ordered = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Long id : questionIds) {
            if (id == null) throw new RuntimeException("题目ID无效");
            if (!seen.add(id)) throw new RuntimeException("题目ID重复");
            TestQuestion q = map.get(id);
            if (q == null) throw new RuntimeException("题目不存在");
            ordered.add(q);
        }

        boolean autoAssemble = rules != null && Boolean.TRUE.equals(rules.getAutoAssemble());
        if (!autoAssemble) {
            for (TestQuestion q : ordered) {
                if (q.getPaperId() == null || !q.getPaperId().equals(paperId)) {
                    throw new RuntimeException("题目不属于当前试卷");
                }
            }
            return ordered;
        }

        List<TestPaper> enabled = paperMapper.selectList(
                new LambdaQueryWrapper<TestPaper>().eq(TestPaper::getEnabled, 1)
        );
        Set<Long> enabledPaperIds = enabled.stream().map(TestPaper::getId).collect(Collectors.toSet());
        for (TestQuestion q : ordered) {
            if (q.getPaperId() == null || !enabledPaperIds.contains(q.getPaperId())) {
                throw new RuntimeException("题目无效");
            }
        }
        return ordered;
    }

    private List<TestQuestion> assembleQuestions(TestPaper selectedPaper, TestRulesService.TestRules rules) {
        int target = rules.getQuestionCount() == null ? 10 : rules.getQuestionCount();

        List<TestPaper> enabledPapers = paperMapper.selectList(
                new LambdaQueryWrapper<TestPaper>()
                        .eq(TestPaper::getEnabled, 1)
        );
        if (enabledPapers.isEmpty()) return new ArrayList<>();

        Set<String> selectedTags = new HashSet<>(splitTags(selectedPaper.getTags()));

        List<TestPaper> candidatePapers = enabledPapers;
        if (!selectedTags.isEmpty()) {
            candidatePapers = enabledPapers.stream()
                    .filter(p -> hasAnyTagOverlap(selectedTags, splitTags(p.getTags())))
                    .collect(Collectors.toList());
            if (candidatePapers.isEmpty()) candidatePapers = enabledPapers;
        }

        Map<Long, String> paperDifficultyMap = new HashMap<>();
        List<Long> paperIds = new ArrayList<>();
        for (TestPaper p : candidatePapers) {
            paperIds.add(p.getId());
            paperDifficultyMap.put(p.getId(), normalizeDifficulty(p.getDifficulty()));
        }

        List<TestQuestion> all = questionMapper.selectList(
                new LambdaQueryWrapper<TestQuestion>()
                        .in(TestQuestion::getPaperId, paperIds)
        );
        if (all.isEmpty()) return new ArrayList<>();

        int total = Math.min(target, all.size());

        int easyN = (int) Math.floor(total * (rules.getEasyPct() == null ? 45 : rules.getEasyPct()) / 100.0);
        int mediumN = (int) Math.floor(total * (rules.getMediumPct() == null ? 40 : rules.getMediumPct()) / 100.0);
        int hardN = total - easyN - mediumN;

        List<TestQuestion> easy = new ArrayList<>();
        List<TestQuestion> medium = new ArrayList<>();
        List<TestQuestion> hard = new ArrayList<>();

        for (TestQuestion q : all) {
            String d = paperDifficultyMap.getOrDefault(q.getPaperId(), "EASY");
            if ("HARD".equals(d)) hard.add(q);
            else if ("MEDIUM".equals(d)) medium.add(q);
            else easy.add(q);
        }

        Set<Long> chosen = new HashSet<>();
        pickRandomIds(easy, easyN, chosen);
        pickRandomIds(medium, mediumN, chosen);
        pickRandomIds(hard, hardN, chosen);

        if (chosen.size() < total) {
            List<TestQuestion> rest = new ArrayList<>();
            for (TestQuestion q : all) {
                if (!chosen.contains(q.getId())) rest.add(q);
            }
            pickRandomIds(rest, total - chosen.size(), chosen);
        }

        List<Long> ids = new ArrayList<>(chosen);
        if (ids.isEmpty()) return new ArrayList<>();

        List<TestQuestion> picked = questionMapper.selectList(
                new LambdaQueryWrapper<TestQuestion>()
                        .in(TestQuestion::getId, ids)
        );
        picked.sort(Comparator
                .comparing((TestQuestion q) -> difficultyRank(paperDifficultyMap.getOrDefault(q.getPaperId(), "EASY")))
                .thenComparing(TestQuestion::getPaperId)
                .thenComparing(q -> q.getSortNo() == null ? Integer.MAX_VALUE : q.getSortNo())
                .thenComparing(TestQuestion::getId)
        );
        return picked;
    }

    private void pickRandomIds(List<TestQuestion> pool, int count, Set<Long> out) {
        if (count <= 0 || pool.isEmpty()) return;
        List<TestQuestion> copy = new ArrayList<>(pool);
        Collections.shuffle(copy);
        int need = count;
        for (TestQuestion q : copy) {
            if (need <= 0) break;
            if (out.add(q.getId())) need--;
        }
    }

    private boolean hasAnyTagOverlap(Set<String> a, List<String> b) {
        if (a == null || a.isEmpty() || b == null || b.isEmpty()) return false;
        for (String t : b) {
            if (a.contains(t)) return true;
        }
        return false;
    }

    private String normalizeDifficulty(String d) {
        String s = d == null ? "" : d.trim().toUpperCase();
        if ("HARD".equals(s) || "MEDIUM".equals(s) || "EASY".equals(s)) return s;
        return "EASY";
    }

    private int difficultyRank(String d) {
        if ("EASY".equals(d)) return 1;
        if ("MEDIUM".equals(d)) return 2;
        if ("HARD".equals(d)) return 3;
        return 4;
    }
}
