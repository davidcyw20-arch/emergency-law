package com.yunxian.emergencylaw.test.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.test.entity.TestPaper;
import com.yunxian.emergencylaw.test.entity.TestQuestion;
import com.yunxian.emergencylaw.test.mapper.TestPaperMapper;
import com.yunxian.emergencylaw.test.mapper.TestQuestionMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestBankSeedService {

    private final TestPaperMapper paperMapper;
    private final TestQuestionMapper questionMapper;

    @Transactional
    public SeedResult seed(boolean force) {
        SeedResult r = new SeedResult();

        TestPaper easy = ensurePaper("应急普法基础（简单）", "EASY", "应急,普法,基础", 45, force, r);
        TestPaper medium = ensurePaper("应急普法提升（中等）", "MEDIUM", "应急,普法,提升", 60, force, r);
        TestPaper hard = ensurePaper("应急普法强化（困难）", "HARD", "应急,普法,强化", 75, force, r);

        r.easyQuestionsAdded = ensureQuestions(easy.getId(), 30, buildEasySeeds(), force);
        r.mediumQuestionsAdded = ensureQuestions(medium.getId(), 30, buildMediumSeeds(), force);
        r.hardQuestionsAdded = ensureQuestions(hard.getId(), 30, buildHardSeeds(), force);

        return r;
    }

    private TestPaper ensurePaper(String title, String difficulty, String tags, int durationMin, boolean force, SeedResult r) {
        TestPaper exist = paperMapper.selectOne(
                new LambdaQueryWrapper<TestPaper>().eq(TestPaper::getTitle, title).last("limit 1")
        );
        if (exist != null) {
            if (force) {
                exist.setDifficulty(difficulty);
                exist.setTags(tags);
                exist.setDurationMin(durationMin);
                exist.setEnabled(1);
                paperMapper.updateById(exist);
            }
            return exist;
        }

        TestPaper p = new TestPaper();
        p.setTitle(title);
        p.setDifficulty(difficulty);
        p.setTags(tags);
        p.setDurationMin(durationMin);
        p.setEnabled(1);
        p.setCreatedAt(LocalDateTime.now());
        paperMapper.insert(p);
        r.papersCreated++;
        return p;
    }

    private int ensureQuestions(Long paperId, int target, List<SeedQuestion> seeds, boolean force) {
        Long cnt = questionMapper.selectCount(new LambdaQueryWrapper<TestQuestion>().eq(TestQuestion::getPaperId, paperId));
        int existing = cnt == null ? 0 : Math.toIntExact(cnt);
        int need = force ? Math.max(0, target) : Math.max(0, target - existing);
        if (need <= 0) return 0;

        int sortNo = existing + 1;
        int added = 0;
        for (int i = 0; i < need; i++) {
            SeedQuestion s = seeds.get(i % seeds.size());
            String stem = s.stem;
            if (i >= seeds.size()) {
                stem = stem + "（扩展题 " + (i + 1) + "）";
            }

            TestQuestion q = new TestQuestion();
            q.setPaperId(paperId);
            q.setStem(stem);
            q.setOptionA(s.a);
            q.setOptionB(s.b);
            q.setOptionC(s.c);
            q.setOptionD(s.d);
            q.setAnswerIndex(s.answerIndex);
            q.setSortNo(sortNo++);
            q.setCreatedAt(LocalDateTime.now());
            questionMapper.insert(q);
            added++;
        }
        return added;
    }

    private List<SeedQuestion> buildEasySeeds() {
        return Arrays.asList(
                SeedQuestion.tf("发现火情应先确认自身安全再报警/上报。", true),
                SeedQuestion.tf("遇到浓烟时应尽量直立奔跑快速通过。", false),
                SeedQuestion.tf("地震发生时优先就近躲避，远离玻璃和吊灯。", true),
                SeedQuestion.tf("应急避难场所标识一般为绿色并带有图形符号。", true),
                SeedQuestion.tf("电梯是火灾逃生的首选工具。", false),
                SeedQuestion.sc("发生火灾时，下列做法正确的是：", "乘坐电梯快速下楼", "用湿毛巾捂口鼻、低姿撤离", "返回取贵重物品", "躲在床底等待救援", 1),
                SeedQuestion.sc("应急物资储备中，最重要的是：", "高热量食品和饮水", "高跟鞋", "奢侈品", "大量香水", 0),
                SeedQuestion.sc("手机收到暴雨红色预警时，建议：", "继续前往河道游玩", "远离低洼地/地下空间", "在桥洞下避雨", "进入山谷露营", 1),
                SeedQuestion.sc("遇到燃气泄漏应：", "立即开灯检查", "打开窗户通风并关闭阀门", "点火测试是否泄漏", "在室内拨打电话不通风", 1),
                SeedQuestion.sc("发现有人触电，正确的第一步是：", "直接徒手拉开", "先切断电源或用绝缘物隔离", "立即泼水降温", "先拍照发朋友圈", 1)
        );
    }

    private List<SeedQuestion> buildMediumSeeds() {
        return Arrays.asList(
                SeedQuestion.sc("突发事件信息报告的原则通常不包括：", "及时", "准确", "全面", "夸大", 3),
                SeedQuestion.sc("应急演练的主要目的之一是：", "制造恐慌", "检验预案可行性并发现问题", "增加工作量", "替代真实救援", 1),
                SeedQuestion.sc("进入人员密集场所，首先应留意：", "灯光颜色", "安全出口与疏散通道", "装饰风格", "背景音乐", 1),
                SeedQuestion.sc("洪水来临时，以下哪项更安全：", "留在地下室", "转移到高处并远离河道", "在桥下避雨", "涉水过河抄近路", 1),
                SeedQuestion.sc("发生强震后，最先应该：", "立刻跑向楼梯口", "在安全处避险，震后再有序撤离", "跳窗逃生", "返回取物品", 1),
                SeedQuestion.sc("关于公共卫生事件，正确的是：", "出现症状应主动报告并就医", "隐瞒行程最安全", "随意传播未经证实信息", "拒绝必要的防护", 0),
                SeedQuestion.sc("应急广播发布撤离指令后，应：", "逆行围观", "按指引有序撤离不拥挤", "抢先推搡", "先去拍视频", 1),
                SeedQuestion.tf("应急预案需要定期评估和修订。", true),
                SeedQuestion.tf("紧急情况下谣言传播可能增加处置难度。", true),
                SeedQuestion.sc("以下哪项属于应急响应中的资源保障：", "编造数据", "调配救援物资与人员", "拖延处置", "隐瞒风险", 1)
        );
    }

    private List<SeedQuestion> buildHardSeeds() {
        return Arrays.asList(
                SeedQuestion.sc("现场处置中“统一指挥”的意义在于：", "让多头指挥更灵活", "减少冲突、提升协同效率", "延长决策时间", "降低信息共享", 1),
                SeedQuestion.sc("当处置目标与公众安全发生冲突时，应优先：", "形象工程", "公众生命安全", "个人便利", "短期流量", 1),
                SeedQuestion.sc("风险沟通中不推荐的做法是：", "及时披露关键信息", "承认不确定性并持续更新", "使用权威渠道发布", "隐瞒事实以免恐慌", 3),
                SeedQuestion.sc("应急处置中的“分级响应”强调：", "所有事件都用最高等级", "根据影响范围和严重程度匹配资源", "不需要预案", "只靠个人经验", 1),
                SeedQuestion.sc("对危险化学品事故现场，正确做法是：", "随意靠近围观", "设立警戒区并按专业指引处置", "用明火照明查漏", "让无防护人员进入", 1),
                SeedQuestion.sc("信息发布中，避免二次伤害应做到：", "泄露个人隐私", "尊重当事人并保护隐私", "夸张渲染细节", "传播未经核实画面", 1),
                SeedQuestion.tf("应急处置中，信息记录与复盘有助于改进预案。", true),
                SeedQuestion.tf("只要响应结束，就不需要评估和总结。", false),
                SeedQuestion.sc("大型活动安全管理中，最关键的是：", "忽略人流密度", "实时监测人流并保持疏散通道畅通", "临时封堵出口", "只靠口头提醒", 1),
                SeedQuestion.sc("灾后心理援助的正确认识是：", "不需要，忍一忍就好", "必要时提供支持，减少长期心理影响", "越快忘掉越好无需沟通", "只关注物资不关注人", 1)
        );
    }

    @Data
    private static class SeedQuestion {
        private String stem;
        private String a;
        private String b;
        private String c;
        private String d;
        private Integer answerIndex;

        static SeedQuestion tf(String stem, boolean isTrue) {
            SeedQuestion q = new SeedQuestion();
            q.stem = stem;
            q.a = "正确";
            q.b = "错误";
            q.c = "-";
            q.d = "-";
            q.answerIndex = isTrue ? 0 : 1;
            return q;
        }

        static SeedQuestion sc(String stem, String a, String b, String c, String d, int answerIndex) {
            SeedQuestion q = new SeedQuestion();
            q.stem = stem;
            q.a = a;
            q.b = b;
            q.c = c;
            q.d = d;
            q.answerIndex = answerIndex;
            return q;
        }
    }

    @Data
    public static class SeedResult {
        private int papersCreated;
        private int easyQuestionsAdded;
        private int mediumQuestionsAdded;
        private int hardQuestionsAdded;
    }
}

