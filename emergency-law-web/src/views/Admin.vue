
<template>
  <div class="admin">
    <div class="mesh m1"></div>
    <div class="mesh m2"></div>
    <div class="mesh m3"></div>

    <header class="topbar">
      <div class="brand">
        <div class="brandBadge">EL</div>
        <div>
          <div class="brandTitle">应急普法管理控制台</div>
          <div class="brandSub">课程 · 题库 · 试卷规则 · 审核 · 用户 · 报表</div>
        </div>
      </div>

      <div class="topActions">
        <div class="chip">角色：{{ roleLabel }}</div>
        <div class="chip">接口：8080</div>
        
      </div>
    </header>

    <div class="shell">
      <aside class="side">
        <div class="sideTitle">管理模块</div>
        <button
          v-for="item in navItems"
          :key="item.id"
          class="sideItem"
          :class="{ active: activeSection === item.id }"
          @click="jumpTo(item.id)"
        >
          <div class="sideItemTitle">{{ item.title }}</div>
          <div class="sideItemSub">{{ item.sub }}</div>
        </button>

        <div class="sideNote">
          <div class="sideNoteTitle">运营脉冲</div>
          <div class="sideNoteRow">
            <span>待审队列</span>
            <strong>{{ moderationQueue.length }}</strong>
          </div>
          <div class="sideNoteRow">
            <span>草稿</span>
            <strong>{{ draftCount }}</strong>
          </div>
          <div class="sideNoteRow">
            <span>停用用户</span>
            <strong>{{ warningCount }}</strong>
          </div>
        </div>
      </aside>

      <main class="content">
        <section id="overview" class="section">
          <div class="sectionHeader">
            <div>
              <div class="sectionTitle">运营总览</div>
              <div class="sectionSub">内容流转与质量信号实时概览。</div>
            </div>
            <div class="sectionActions">
              <el-button plain :loading="exporting" @click="exportReport">导出</el-button>
              <el-button plain @click="syncData">同步数据</el-button>
              <el-button type="primary" plain @click="jumpTo('moderation')">查看队列</el-button>
            </div>
          </div>

          <div class="statGrid">
            <div v-for="(s, idx) in stats" :key="s.label" class="statCard" :style="{ '--i': idx + 1 }">
              <div class="statLabel">{{ s.label }}</div>
              <div class="statValue">{{ s.value }}</div>
              <div class="statMeta">
                <span class="statDelta" :class="s.tone">{{ s.delta }}</span>
                <span class="statNote">{{ s.note }}</span>
              </div>
            </div>
          </div>
        </section>
        <section id="courses" class="section panel">
          <div class="sectionHeader">
            <div>
              <div class="sectionTitle">课程管理</div>
              <div class="sectionSub">维护课程目录深度、发布节奏与内容质量。</div>
            </div>
            <div class="sectionActions">
              <el-input v-model="courseKeyword" placeholder="搜索课程" clearable />
              <el-select v-model="courseStatus" placeholder="状态" class="selectSlim">
                <el-option label="全部" value="all" />
                <el-option label="已发布" value="已发布" />
                <el-option label="草稿" value="草稿" />
                <el-option label="已归档" value="已归档" />
              </el-select>
              <el-button type="primary" plain @click="openCourseCreate">新建课程</el-button>
            </div>
          </div>

          <el-table :ref="setTableRef" :data="filteredCourses" stripe class="table">
            <el-table-column label="封面" width="120">
              <template #default="{ row }">
                <img
                  :src="normalizeCoverUrl(row.coverUrl) || '/covers/course-default.svg'"
                  alt="cover"
                  style="width: 96px; height: 54px; object-fit: cover; border-radius: 12px; border: 1px solid rgba(15,23,42,0.10)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="title" label="课程标题" min-width="220" />
            <el-table-column label="分类" width="160">
              <template #default="{ row }">
                {{ categoryName(row.categoryId) }}
              </template>
            </el-table-column>
            <el-table-column prop="difficulty" label="难度" width="110" />
            <el-table-column label="状态" width="140">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updated" label="更新日期" width="140" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" plain @click="openCourseEdit(row)">编辑</el-button>
                <el-button size="small" plain @click="openLessonManager(row)">课时</el-button>
                <el-button size="small" type="primary" plain @click="publishCourse(row)">发布</el-button>
                <el-button size="small" type="warning" plain @click="archiveCourse(row)">归档</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
        <section id="questions" class="section panel">
          <div class="sectionHeader">
            <div>
              <div class="sectionTitle">题库管理</div>
              <div class="sectionSub">平衡覆盖范围、难度与题目新鲜度。</div>
            </div>
            <div class="sectionActions">
              <el-input v-model="paperKeyword" placeholder="搜索试卷" clearable />
              <el-select v-model="paperDifficulty" placeholder="难度" class="selectSlim">
                <el-option label="全部" value="all" />
                <el-option label="简单" value="EASY" />
                <el-option label="中等" value="MEDIUM" />
                <el-option label="困难" value="HARD" />
              </el-select>
              <el-button type="warning" plain @click="seedTestBank">补充题库</el-button>
              <el-button plain @click="loadPapers">刷新</el-button>
              <el-button type="primary" plain @click="openPaperCreate">新建试卷</el-button>
            </div>
          </div>

          <el-table :ref="setTableRef" :data="filteredPapers" stripe class="table">
            <el-table-column prop="title" label="试卷标题" min-width="260" />
            <el-table-column label="难度" width="120">
              <template #default="{ row }">{{ paperDifficultyLabel(row.difficulty) }}</template>
            </el-table-column>
            <el-table-column prop="questionCount" label="题数" width="90" />
            <el-table-column prop="durationMin" label="时长(分钟)" width="120" />
            <el-table-column label="状态" width="140">
              <template #default="{ row }">
                <el-tag :type="row.enabled == 1 ? 'success' : 'warning'">{{ row.enabled == 1 ? '已发布' : '草稿' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="300">
              <template #default="{ row }">
                <el-button size="small" plain @click="openPaperEdit(row)">编辑</el-button>
                <el-button size="small" plain @click="openPaperQuestions(row)">题目</el-button>
                <el-button size="small" type="primary" plain :disabled="row.enabled == 1" @click="publishPaper(row)">发布</el-button>
                <el-button size="small" type="warning" plain :disabled="row.enabled != 1" @click="archivePaper(row)">归档</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section id="rules" class="section panel">
          <div class="sectionHeader">
            <div>
              <div class="sectionTitle">试卷规则</div>
              <div class="sectionSub">组卷蓝图与评分策略配置。</div>
            </div>
            <div class="sectionActions">
                            <el-button type="primary" plain @click="saveRules">保存规则</el-button>
            </div>
          </div>

          <div class="ruleGrid">
            <div class="ruleCard">
              <div class="ruleTitle">组卷设置</div>
              <div class="ruleRow">
                <span>自动组卷</span>
                <el-switch v-model="ruleSettings.autoAssemble" />
              </div>
              <div class="ruleRow">
                <span>题目乱序</span>
                <el-switch v-model="ruleSettings.shuffle" />
              </div>
              <div class="ruleRow">
                <span>允许重考</span>
                <el-switch v-model="ruleSettings.allowRetake" />
              </div>
              <div class="ruleRow">
                <span>每次出题数</span>
                <el-input-number v-model="ruleSettings.questionCount" :min="1" :max="200" />
              </div>
            </div>

            <div class="ruleCard">
              <div class="ruleTitle">评分策略</div>
              <div class="ruleMetric">
                <div>
                  <div class="ruleLabel">及格分数</div>
                  <div class="ruleValue">{{ ruleSettings.passScore }} 分</div>
                </div>
                <el-input-number v-model="ruleSettings.passScore" :min="60" :max="100" />
              </div>
              <div class="ruleMetric">
                <div>
                  <div class="ruleLabel">限时</div>
                  <div class="ruleValue">{{ ruleSettings.timeLimit }} 分钟</div>
                </div>
                <el-input-number v-model="ruleSettings.timeLimit" :min="10" :max="120" />
              </div>
              <div class="ruleMetric">
                <div>
                  <div class="ruleLabel">最大次数</div>
                  <div class="ruleValue">{{ ruleSettings.attempts }}</div>
                </div>
                <el-input-number v-model="ruleSettings.attempts" :min="1" :max="5" />
              </div>
            </div>

            <div class="ruleCard">
              <div class="ruleTitle">难度占比</div>
              <div class="mixRow">
                <div class="mixLabel">简单</div>
                <div class="mixBar">
                  <span :style="{ width: (ruleSettings.easyPct || 0) + '%', background: '#14b8a6' }"></span>
                </div>
                <el-input-number v-model="ruleSettings.easyPct" :min="0" :max="100" size="small" />
              </div>
              <div class="mixRow">
                <div class="mixLabel">中等</div>
                <div class="mixBar">
                  <span :style="{ width: (ruleSettings.mediumPct || 0) + '%', background: '#f59e0b' }"></span>
                </div>
                <el-input-number v-model="ruleSettings.mediumPct" :min="0" :max="100" size="small" />
              </div>
              <div class="mixRow">
                <div class="mixLabel">困难</div>
                <div class="mixBar">
                  <span :style="{ width: (ruleSettings.hardPct || 0) + '%', background: '#ef4444' }"></span>
                </div>
                <el-input-number v-model="ruleSettings.hardPct" :min="0" :max="100" size="small" />
              </div>
              <div class="mixHint" :class="{ bad: mixSum !== 100 }">总计：{{ mixSum }}%</div>
            </div>
          </div>
        </section>
        <section id="moderation" class="section panel">
          <div class="sectionHeader">
            <div>
              <div class="sectionTitle">帖子审核</div>
              <div class="sectionSub">结合风险信号与上下文审核内容。</div>
            </div>
            <div class="sectionActions">
              <el-input v-model="moderationTagKeyword" placeholder="按标签搜索帖子" clearable class="selectSlim" />
              <el-button plain @click="autoTriage">自动分流</el-button>
              <el-button type="danger" plain @click="bulkReject">批量驳回</el-button>
            </div>
          </div>

          <el-table :ref="setTableRef" :data="filteredModerationQueue" stripe class="table">
            <el-table-column prop="title" label="标题" min-width="240" />
            <el-table-column prop="author" label="作者" width="140" />
            <el-table-column prop="risk" label="风险" width="110" />
            <el-table-column label="标签" min-width="180">
              <template #default="{ row }">{{ (row.tags || []).join('、') || '-' }}</template>
            </el-table-column>
            <el-table-column prop="commentCount" label="评论" width="90" />
            <el-table-column label="状态" width="150">
              <template #default="{ row }">
                <span class="pill" :class="row.statusKey">{{ row.status }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="time" label="提交时间" width="140" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" type="success" plain @click="approvePost(row)">通过</el-button>
                <el-button size="small" type="danger" plain @click="rejectPost(row)">拒绝</el-button>
                <el-button size="small" plain @click="viewPost(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section id="users" class="section panel">
          <div class="sectionHeader">
            <div>
              <div class="sectionTitle">用户管理</div>
              <div class="sectionSub">权限分配、角色管理与账号健康。</div>
            </div>
            <div class="sectionActions">
              <el-input v-model="userKeyword" placeholder="搜索用户" clearable />
              
            </div>
          </div>

          <el-table :ref="setTableRef" :data="filteredUsers" stripe class="table">
            <el-table-column prop="username" label="用户" min-width="200" />
            <el-table-column label="角色" width="130">
              <template #default="{ row }">
                {{ roleMap[row.role] || row.role }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="140">
              <template #default="{ row }">
                <el-tag :type="row.status === '正常' ? 'success' : 'danger'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="region" label="区域" width="140" />
            <el-table-column prop="lastLogin" label="最近登录" width="160" />
            <el-table-column label="操作" width="260">
              <template #default="{ row }">
                <el-button size="small" plain @click="resetUser(row)">重置</el-button>
                <el-button size="small" plain :disabled="isSelf(row)" @click="toggleUserRole(row)">
                  {{ row.role === 'ADMIN' ? '设为用户' : '设为管理员' }}
                </el-button>
                <el-button size="small" type="warning" plain :disabled="isSelf(row)" @click="toggleUserStatus(row)">
                  {{ row.status === '停用' ? '启用' : '停用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section id="reports" class="section panel">
          <div class="sectionHeader">
            <div>
              <div class="sectionTitle">统计报表</div>
              <div class="sectionSub">活跃度、完成率与合规覆盖趋势。</div>
            </div>
            <div class="sectionActions">
              <el-button plain @click="switchReport('月报')">月报</el-button>
              <el-button plain @click="switchReport('季报')">季报</el-button>
              <el-button type="primary" plain :loading="reportLoading" @click="loadReport">刷新</el-button>
            </div>
          </div>

          <div class="reportGrid">
            <div class="reportCard">
              <div class="reportTitle">{{ reportMode }}概览</div>
              <div class="reportValue">{{ reportData?.title || '-' }}</div>
              <div class="reportNote">周期：{{ reportRangeText }}</div>
            </div>
            <div class="reportCard">
              <div class="reportTitle">新增用户</div>
              <div class="reportValue">{{ reportData?.newUsers ?? 0 }}</div>
              <div class="reportNote">停用用户（全局）：{{ overview?.disabledUserCount ?? 0 }}</div>
            </div>
            <div class="reportCard">
              <div class="reportTitle">新增帖子</div>
              <div class="reportValue">{{ reportData?.newPosts ?? 0 }}</div>
              <div class="reportNote">其中隐藏：{{ reportData?.hiddenPosts ?? 0 }}</div>
            </div>
            <div class="reportCard">
              <div class="reportTitle">测验次数</div>
              <div class="reportValue">{{ reportData?.testSubmissions ?? 0 }}</div>
              <div class="reportNote">试卷（全局）：{{ overview?.paperCount ?? 0 }}</div>
            </div>
            <div class="reportCard">
              <div class="reportTitle">学习活跃</div>
              <div class="reportValue">{{ reportData?.activeLearners ?? 0 }}</div>
              <div class="reportNote">有学习行为的用户数（按日去重）</div>
            </div>
          </div>

          <div style="margin-top: 14px">
            <el-table :ref="setTableRef" :data="reportTableRows" stripe class="table">
              <el-table-column prop="date" label="日期" width="120" />
              <el-table-column prop="newUsers" label="新增用户" width="110" />
              <el-table-column prop="newPosts" label="新增帖子" width="110" />
              <el-table-column prop="hiddenPosts" label="隐藏" width="90" />
              <el-table-column prop="testSubmissions" label="测验次数" width="110" />
              <el-table-column prop="activeLearners" label="活跃学习" width="110" />
            </el-table>
          </div>
        </section>
      </main>
    </div>

    <el-dialog v-model="courseDialogVisible" :title="courseDialogTitle" width="520px">
      <el-form :model="courseForm" label-position="top">
        <el-form-item label="课程标题">
          <el-input v-model="courseForm.title" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="courseForm.categoryId" placeholder="请选择分类" class="selectSlim" style="width: 100%">
            <el-option v-for="c in courseCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面">
          <div style="display: grid; grid-template-columns: 1fr 120px; gap: 10px; align-items: start">
            <el-input v-model="courseForm.coverUrl" placeholder="例如：/covers/xxx.jpg 或 https://..." />
            <el-button plain @click="courseForm.coverUrl = '/covers/course-default.svg'">使用默认</el-button>
          </div>
          <div style="margin-top: 10px; border-radius: 14px; overflow: hidden; border: 1px solid rgba(15,23,42,0.10)">
            <img
              :src="courseCoverPreviewUrl"
              alt="cover"
              style="width: 100%; height: 120px; object-fit: cover; display: block"
            />
          </div>
          <div style="margin-top: 8px; color: rgba(15,23,42,0.55); font-size: 12px">
            提示：把图片放到 `emergency-law-web/public/covers/`，然后填 `/covers/文件名`。
          </div>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="courseForm.summary" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="courseForm.difficulty" class="selectSlim">
            <el-option label="简单" value="简单" />
            <el-option label="中等" value="中等" />
            <el-option label="困难" value="困难" />
          </el-select>
        </el-form-item>
        <el-form-item label="立即发布">
          <el-switch v-model="courseForm.publishNow" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="courseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="paperDialogVisible" :title="paperDialogTitle" width="720px">
      <el-form :model="paperForm" label-position="top">
        <el-form-item label="试卷标题">
          <el-input v-model="paperForm.title" />
        </el-form-item>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px">
          <el-form-item label="难度">
            <el-select v-model="paperForm.difficulty" class="selectSlim">
              <el-option label="简单(EASY)" value="EASY" />
              <el-option label="中等(MEDIUM)" value="MEDIUM" />
              <el-option label="困难(HARD)" value="HARD" />
            </el-select>
          </el-form-item>
          <el-form-item label="时长(分钟)">
            <el-input-number v-model="paperForm.durationMin" :min="5" :max="240" />
          </el-form-item>
        </div>
        <el-form-item label="标签（逗号分隔）">
          <el-input v-model="paperForm.tags" placeholder="例如：基础,应急响应" />
        </el-form-item>
        <el-form-item label="立即发布">
          <el-switch v-model="paperForm.publishNow" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paperDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePaper">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="paperQuestionsDialogVisible" :title="`题目管理：${paperQuestionsTitle}`" width="980px">
      <div style="display: flex; justify-content: space-between; gap: 10px; margin-bottom: 10px; flex-wrap: wrap">
        <div style="color: rgba(15, 23, 42, 0.6); font-size: 13px">
          说明：用户端在线测试来自 test_paper / test_question。
        </div>
        <div style="display: flex; gap: 10px">
          <el-button plain @click="loadPaperQuestions">刷新</el-button>
          <el-button type="primary" plain @click="openQuestionCreate">新增题目</el-button>
        </div>
      </div>

      <el-table :ref="setTableRef" :data="paperQuestionRows" stripe class="table">
        <el-table-column prop="sortNo" label="排序" width="80" />
        <el-table-column prop="stem" label="题干" min-width="260" />
        <el-table-column label="答案" width="120">
          <template #default="{ row }">{{ answerLabel(row.answerIndex) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" plain @click="openQuestionEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="deleteQuestion(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="questionDialogVisible" :title="questionDialogTitle" width="760px">
      <el-form :model="questionForm" label-position="top">
        <el-form-item label="题型">
          <el-select v-model="questionForm.type" class="selectSlim" style="width: 220px">
            <el-option label="单选题" value="SINGLE" />
            <el-option label="判断题" value="TF" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干">
          <el-input v-model="questionForm.stem" type="textarea" :rows="3" />
        </el-form-item>
        <div v-if="questionForm.type !== 'TF'" style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px">
          <el-form-item label="选项 A">
            <el-input v-model="questionForm.optionA" />
          </el-form-item>
          <el-form-item label="选项 B">
            <el-input v-model="questionForm.optionB" />
          </el-form-item>
          <el-form-item label="选项 C">
            <el-input v-model="questionForm.optionC" />
          </el-form-item>
          <el-form-item label="选项 D">
            <el-input v-model="questionForm.optionD" />
          </el-form-item>
        </div>
        <div v-else style="margin: 6px 0 12px; color: rgba(15, 23, 42, 0.62); font-size: 13px">
          判断题固定选项：A=正确，B=错误。
        </div>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px">
          <el-form-item label="正确答案">
            <el-select v-model="questionForm.answerIndex" class="selectSlim">
              <template v-if="questionForm.type === 'TF'">
                <el-option label="正确" :value="0" />
                <el-option label="错误" :value="1" />
              </template>
              <template v-else>
                <el-option label="A" :value="0" />
                <el-option label="B" :value="1" />
                <el-option label="C" :value="2" />
                <el-option label="D" :value="3" />
              </template>
            </el-select>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="questionForm.sortNo" :min="1" :max="9999" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="questionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuestion">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="lessonsDialogVisible" :title="`课时管理：${lessonsCourseTitle}`" width="820px">
      <div style="display: flex; justify-content: space-between; gap: 10px; margin-bottom: 10px; flex-wrap: wrap">
        <div style="color: rgba(15, 23, 42, 0.6); font-size: 13px">
          说明：用户端课程内容来自课时（TEXT/LINK/VIDEO）。
        </div>
        <div style="display: flex; gap: 10px">
          <el-button plain @click="loadLessons">刷新</el-button>
          <el-button type="primary" plain @click="openLessonCreate">新增课时</el-button>
        </div>
      </div>

      <el-table :ref="setTableRef" :data="lessonRows" stripe class="table">
        <el-table-column prop="title" label="课时标题" min-width="220" />
        <el-table-column prop="contentType" label="类型" width="110" />
        <el-table-column prop="durationMin" label="时长(分钟)" width="110" />
        <el-table-column prop="sortNo" label="排序" width="90" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" plain @click="openLessonEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="deleteLesson(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="lessonEditDialogVisible" :title="lessonEditDialogTitle" width="720px">
      <el-form :model="lessonForm" label-position="top">
        <el-form-item label="课时标题">
          <el-input v-model="lessonForm.title" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="lessonForm.contentType" class="selectSlim">
            <el-option label="文本(TEXT)" value="TEXT" />
            <el-option label="链接(LINK)" value="LINK" />
            <el-option label="视频(VIDEO)" value="VIDEO" />
          </el-select>
        </el-form-item>
        <el-form-item label="文本内容" v-if="lessonForm.contentType === 'TEXT'">
          <el-input v-model="lessonForm.contentText" type="textarea" :rows="8" placeholder="请输入课程文本内容" />
        </el-form-item>
        <el-form-item :label="lessonForm.contentType === 'VIDEO' ? '视频地址或链接' : '链接地址'" v-else>
          <el-input v-model="lessonForm.contentUrl" :placeholder="lessonForm.contentType === 'VIDEO' ? '支持 mp4/m3u8 或第三方视频页链接' : 'https://...'" />
          <div v-if="lessonForm.contentType === 'VIDEO'" class="ruleLabel" style="margin-top:6px">可填写视频直链或在线视频页面链接，用户端会自动展示“打开视频链接”。</div>
        </el-form-item>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px">
          <el-form-item label="时长(分钟)">
            <el-input-number v-model="lessonForm.durationMin" :min="1" :max="240" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="lessonForm.sortNo" :min="0" :max="9999" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="lessonEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveLesson">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="postDetailVisible" title="帖子详情" width="760px">
      <div v-if="currentPost" class="postDetailWrap">
        <div class="postDetailTitle">{{ currentPost.title }}</div>
        <div class="postDetailMeta">
          <span>作者：{{ currentPost.author || '匿名' }}</span>
          <span>状态：{{ currentPost.status }}</span>
          <span>时间：{{ currentPost.time || '-' }}</span>
        </div>
        <div class="postDetailTags">
          <el-tag v-for="t in (currentPost.tags || [])" :key="t" size="small" effect="light">{{ t }}</el-tag>
        </div>
        <div class="postDetailContent">{{ currentPost.content || '（暂无正文）' }}</div>
      </div>
      <template #footer>
        <el-button @click="postDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  apiAdminArchiveCourse,
  apiAdminCourses,
  apiAdminCreateCourse,
  apiAdminCreateLesson,
  apiAdminCreateTestPaper,
  apiAdminCreateTestQuestion,
  apiAdminDeleteLesson,
  apiAdminDeleteTestQuestion,
  apiAdminDisableUser,
  apiAdminEnableUser,
  apiAdminLessons,
  apiAdminGetTestRules,
  apiAdminOverviewReport,
  apiAdminMonthlyReport,
  apiAdminQuarterlyReport,
  apiAdminSeedTestBank,
  apiAdminPublishCourse,
  apiAdminPublishTestPaper,
  apiAdminRejectSharePost,
  apiAdminApproveSharePost,
  apiAdminArchiveTestPaper,
  apiAdminSharePosts,
  apiAdminTestPapers,
  apiAdminTestQuestions,
  apiAdminResetUserPassword,
  apiAdminSaveTestRules,
  apiAdminUpdateLesson,
  apiAdminUpdateTestPaper,
  apiAdminUpdateTestQuestion,
  apiAdminUpdateCourse,
  apiAdminUpdateUserRole,
  apiAdminUsers
} from '../api/admin'
import { apiCategories } from '../api/learn'

const me = ref(null)

function isSelf(row) {
  return String(row?.id) === String(me.value?.userId)
}

const roleMap = {
  ADMIN: '管理员',
  EDITOR: '编辑',
  USER: '用户'
}
const roleLabel = computed(() => roleMap[me.value?.role] || '管理员')

const navItems = [
  { id: 'overview', title: '运营总览', sub: '核心指标概览' },
  { id: 'courses', title: '课程管理', sub: '课程与课时' },
  { id: 'questions', title: '题库管理', sub: '覆盖与难度' },
  { id: 'rules', title: '试卷规则', sub: '组卷与评分' },
  { id: 'moderation', title: '帖子审核', sub: '风险与队列' },
  { id: 'users', title: '用户管理', sub: '角色与权限' },
  { id: 'reports', title: '统计报表', sub: '活跃趋势' }
]

const activeSection = ref('overview')

function jumpTo(id) {
  activeSection.value = id
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const overview = ref(null)
const reportMode = ref('月报')
const reportData = ref(null)
const reportLoading = ref(false)
const tableRefs = ref([])
let resizeTimer = null

function setTableRef(el) {
  if (!el) return
  if (!tableRefs.value.includes(el)) tableRefs.value.push(el)
}

function relayoutTables() {
  for (const table of tableRefs.value) {
    try { table?.doLayout?.() } catch (e) { /* ignore */ }
  }
}

function handleResize() {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    nextTick(() => relayoutTables())
  }, 120)
}
const exporting = ref(false)

const reportRangeText = computed(() => {
  const start = reportData.value?.rangeStart
  const endEx = reportData.value?.rangeEndExclusive
  if (!start || !endEx) return '-'
  return `${start} ~ ${endEx}（不含）`
})

const reportTableRows = computed(() => {
  const rows = reportData.value?.daily || []
  const active = rows.filter((r) => {
    const nu = Number(r.newUsers || 0)
    const np = Number(r.newPosts || 0)
    const hp = Number(r.hiddenPosts || 0)
    const ts = Number(r.testSubmissions || 0)
    const al = Number(r.activeLearners || 0)
    return (nu + np + hp + ts + al) > 0
  })
  return active.slice().reverse() // 只展示有活动的日期，且让最新日期在前
})

const stats = computed(() => {
  const o = overview.value || {}
  return [
    { label: '课程（已发布）', value: `${o.publishedCourseCount ?? 0}`, delta: `/${o.courseCount ?? 0}`, note: 'course', tone: 'up' },
    { label: '试卷（已发布）', value: `${o.publishedPaperCount ?? 0}`, delta: `/${o.paperCount ?? 0}`, note: 'test', tone: 'up' },
    { label: '帖子（可见）', value: `${o.visiblePostCount ?? 0}`, delta: `/${o.postCount ?? 0}`, note: 'share', tone: 'up' },
    { label: '测验记录', value: `${o.testRecordCount ?? 0}`, delta: '', note: 'records', tone: 'up' }
  ]
})

const courseKeyword = ref('')
const courseStatus = ref('all')
const courseRows = ref([])
const courseCategories = ref([])

const categoryNameMap = computed(() => {
  const map = {}
  for (const c of courseCategories.value || []) map[c.id] = c.name
  return map
})

function categoryName(id) {
  if (id === null || id === undefined) return '未分类'
  return categoryNameMap.value[id] || `分类 ${id}`
}

const filteredCourses = computed(() => {
  const k = courseKeyword.value.trim().toLowerCase()
  return courseRows.value.filter((row) => {
    const hit =
      !k ||
      row.title.toLowerCase().includes(k) ||
      categoryName(row.categoryId).toLowerCase().includes(k)
    const statusOk = courseStatus.value === 'all' || row.status === courseStatus.value
    return hit && statusOk
  })
})

const paperKeyword = ref('')
const paperDifficulty = ref('all')
const paperRows = ref([])

function paperDifficultyLabel(v) {
  const x = String(v || '').toUpperCase()
  if (x === 'EASY') return '简单'
  if (x === 'MEDIUM') return '中等'
  if (x === 'HARD') return '困难'
  return x || '未知'
}

const filteredPapers = computed(() => {
  const k = paperKeyword.value.trim().toLowerCase()
  return paperRows.value.filter((row) => {
    const hit = !k || String(row.title || '').toLowerCase().includes(k)
    const diffOk = paperDifficulty.value === 'all' || String(row.difficulty || '').toUpperCase() === paperDifficulty.value
    return hit && diffOk
  })
})

const ruleSettings = ref({
  autoAssemble: true,
  shuffle: true,
  allowRetake: true,
  passScore: 80,
  timeLimit: 60,
  attempts: 3,
  questionCount: 10,
  easyPct: 45,
  mediumPct: 40,
  hardPct: 15
})

const mixSum = computed(() =>
  Number(ruleSettings.value?.easyPct || 0) +
  Number(ruleSettings.value?.mediumPct || 0) +
  Number(ruleSettings.value?.hardPct || 0)
)

const moderationQueue = ref([])
const moderationTagKeyword = ref('')
const filteredModerationQueue = computed(() => {
  const k = moderationTagKeyword.value.trim().toLowerCase()
  if (!k) return moderationQueue.value
  return moderationQueue.value.filter((row) => (row.tags || []).some((t) => String(t || '').toLowerCase().includes(k)))
})
const postDetailVisible = ref(false)
const currentPost = ref(null)

const userKeyword = ref('')
const userRows = ref([])

const filteredUsers = computed(() => {
  const k = userKeyword.value.trim().toLowerCase()
  return userRows.value.filter((row) => {
    const u = String(row.username || '').toLowerCase()
    const r = String(row.role || '').toLowerCase()
    return !k || u.includes(k) || r.includes(k)
  })
})

const draftCount = computed(() => courseRows.value.filter((c) => c.status === '草稿').length)
const warningCount = computed(() => userRows.value.filter((u) => u.status === '停用').length)

const courseDialogVisible = ref(false)
const courseDialogMode = ref('create')
const editingCourseId = ref(null)
const courseForm = ref({
  title: '',
  categoryId: null,
  coverUrl: '/covers/course-default.svg',
  summary: '',
  difficulty: '简单',
  publishNow: true
})
const courseDialogTitle = computed(() => (courseDialogMode.value === 'create' ? '新建课程' : '编辑课程'))

function normalizeCoverUrl(input) {
  const u = String(input || '').trim()
  if (!u) return '/covers/course-default.svg'
  if (/^[a-zA-Z]:\\/.test(u) || u.includes(':\\')) return null
  if (u.startsWith('http://') || u.startsWith('https://') || u.startsWith('data:')) return u
  if (u.startsWith('/')) return u
  return `/${u.replace(/^\.?\//, '')}`
}

const courseCoverPreviewUrl = computed(() => {
  return normalizeCoverUrl(courseForm.value.coverUrl) || '/covers/course-default.svg'
})

const paperDialogVisible = ref(false)
const paperDialogMode = ref('create')
const editingPaperId = ref(null)
const paperForm = ref({
  title: '',
  difficulty: 'EASY',
  durationMin: 10,
  tags: '',
  publishNow: true
})
const paperDialogTitle = computed(() => (paperDialogMode.value === 'create' ? '新建试卷' : '编辑试卷'))

const paperQuestionsDialogVisible = ref(false)
const paperQuestionsPaperId = ref(null)
const paperQuestionsTitle = ref('')
const paperQuestionRows = ref([])

function answerLabel(i) {
  const x = Number(i)
  if (x === 0) return 'A'
  if (x === 1) return 'B'
  if (x === 2) return 'C'
  if (x === 3) return 'D'
  return '-'
}

const questionDialogVisible = ref(false)
const questionDialogMode = ref('create')
const editingQuestionId = ref(null)
const questionForm = ref({
  type: 'SINGLE', // SINGLE / TF(判断题)
  stem: '',
  optionA: '',
  optionB: '',
  optionC: '',
  optionD: '',
  answerIndex: 0,
  sortNo: 1
})
const questionDialogTitle = computed(() => (questionDialogMode.value === 'create' ? '新增题目' : '编辑题目'))

const lessonsDialogVisible = ref(false)
const lessonsCourseId = ref(null)
const lessonsCourseTitle = ref('')
const lessonRows = ref([])

const lessonEditDialogVisible = ref(false)
const lessonEditMode = ref('create')
const editingLessonId = ref(null)
const lessonForm = ref({
  title: '',
  contentType: 'TEXT',
  contentText: '',
  contentUrl: '',
  durationMin: 10,
  sortNo: 0
})
const lessonEditDialogTitle = computed(() => (lessonEditMode.value === 'create' ? '新增课时' : '编辑课时'))

function formatDate(date = new Date()) {
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${d}`
}

function statusType(status) {
  if (status === '已发布') return 'success'
  if (status === '草稿') return 'warning'
  return 'info'
}

function openCourseCreate() {
  courseDialogMode.value = 'create'
  editingCourseId.value = null
  const firstId = courseCategories.value?.[0]?.id ?? null
  courseForm.value = { title: '', categoryId: firstId, coverUrl: '/covers/course-default.svg', summary: '', difficulty: '简单', publishNow: true }
  courseDialogVisible.value = true
}

function openCourseEdit(row) {
  courseDialogMode.value = 'edit'
  editingCourseId.value = row.id
  courseForm.value = {
    title: row.title || '',
    categoryId: row.categoryId ?? null,
    coverUrl: row.coverUrl || '/covers/course-default.svg',
    summary: row.summary || '',
    difficulty: row.difficulty || '简单',
    publishNow: row.status === '已发布'
  }
  courseDialogVisible.value = true
}

async function saveCourse() {
  if (!courseForm.value.title.trim()) {
    ElMessage.warning('请填写课程标题')
    return
  }
  if (courseForm.value.categoryId === null || courseForm.value.categoryId === undefined) {
    ElMessage.warning('请选择课程分类')
    return
  }
  const cover = normalizeCoverUrl(courseForm.value.coverUrl)
  if (!cover) {
    ElMessage.warning('封面地址不能是本地磁盘路径（例如 E:\\\\xx\\\\xx.jpg），请放到 public/covers 或使用 http(s) 链接')
    return
  }
  try {
    const payload = {
      title: courseForm.value.title,
      categoryId: courseForm.value.categoryId,
      summary: courseForm.value.summary,
      coverUrl: cover,
      difficulty: courseForm.value.difficulty,
      enabled: courseForm.value.publishNow ? 1 : 0,
      sortNo: 0
    }
    const res =
      courseDialogMode.value === 'create'
        ? await apiAdminCreateCourse(payload)
        : await apiAdminUpdateCourse(editingCourseId.value, payload)

    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '保存失败')
      return
    }

    ElMessage.success(courseDialogMode.value === 'create' ? '课程已创建' : '课程已更新')
    courseDialogVisible.value = false
    await loadCourses()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  }
}

async function publishCourse(row) {
  try {
    const res = await apiAdminPublishCourse(row.id)
    if (!res.data?.success) return ElMessage.error(res.data?.message || '发布失败')
    ElMessage.success('课程已发布')
    await loadCourses()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '发布失败')
  }
}

async function archiveCourse(row) {
  try {
    const res = await apiAdminArchiveCourse(row.id)
    if (!res.data?.success) return ElMessage.error(res.data?.message || '归档失败')
    ElMessage.success('课程已归档')
    await loadCourses()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '归档失败')
  }
}

async function loadPapers() {
  try {
    const res = await apiAdminTestPapers()
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '加载试卷失败')
      return
    }
    paperRows.value = res.data.data || []
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载试卷失败')
  }
}

async function seedTestBank() {
  try {
    const res = await apiAdminSeedTestBank()
    if (!res.data?.success) return ElMessage.error(res.data?.message || '补充失败')
    ElMessage.success('题库已补充')
    await loadPapers()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '补充失败')
  }
}

function openPaperCreate() {
  paperDialogMode.value = 'create'
  editingPaperId.value = null
  paperForm.value = { title: '', difficulty: 'EASY', durationMin: 10, tags: '', publishNow: true }
  paperDialogVisible.value = true
}

function openPaperEdit(row) {
  paperDialogMode.value = 'edit'
  editingPaperId.value = row.id
  paperForm.value = {
    title: row.title || '',
    difficulty: String(row.difficulty || 'EASY').toUpperCase(),
    durationMin: row.durationMin ?? 10,
    tags: row.tags || '',
    publishNow: row.enabled == 1
  }
  paperDialogVisible.value = true
}

async function savePaper() {
  if (!String(paperForm.value.title || '').trim()) {
    ElMessage.warning('请填写试卷标题')
    return
  }
  try {
    const payload = {
      title: paperForm.value.title,
      difficulty: paperForm.value.difficulty,
      durationMin: paperForm.value.durationMin,
      tags: paperForm.value.tags,
      enabled: paperForm.value.publishNow ? 1 : 0
    }
    const res =
      paperDialogMode.value === 'create'
        ? await apiAdminCreateTestPaper(payload)
        : await apiAdminUpdateTestPaper(editingPaperId.value, payload)

    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '保存失败')
      return
    }

    ElMessage.success(paperDialogMode.value === 'create' ? '试卷已创建' : '试卷已更新')
    paperDialogVisible.value = false
    await loadPapers()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  }
}

async function publishPaper(row) {
  try {
    const res = await apiAdminPublishTestPaper(row.id)
    if (!res.data?.success) return ElMessage.error(res.data?.message || '发布失败')
    ElMessage.success('试卷已发布')
    await loadPapers()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '发布失败')
  }
}

async function archivePaper(row) {
  try {
    const res = await apiAdminArchiveTestPaper(row.id)
    if (!res.data?.success) return ElMessage.error(res.data?.message || '归档失败')
    ElMessage.success('试卷已归档')
    await loadPapers()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '归档失败')
  }
}

async function openPaperQuestions(row) {
  paperQuestionsPaperId.value = row.id
  paperQuestionsTitle.value = row.title || `试卷 ${row.id}`
  paperQuestionsDialogVisible.value = true
  await loadPaperQuestions()
}

async function loadPaperQuestions() {
  if (!paperQuestionsPaperId.value) return
  try {
    const res = await apiAdminTestQuestions(paperQuestionsPaperId.value)
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '加载题目失败')
      return
    }
    paperQuestionRows.value = res.data.data || []
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载题目失败')
  }
}

function openQuestionCreate() {
  if (!paperQuestionsPaperId.value) return
  questionDialogMode.value = 'create'
  editingQuestionId.value = null
  questionForm.value = { type: 'SINGLE', stem: '', optionA: '', optionB: '', optionC: '', optionD: '', answerIndex: 0, sortNo: 1 }
  questionDialogVisible.value = true
}

function openQuestionEdit(row) {
  questionDialogMode.value = 'edit'
  editingQuestionId.value = row.id
  const isTF =
    String(row.optionA || '') === '正确' &&
    String(row.optionB || '') === '错误' &&
    String(row.optionC || '') === '-' &&
    String(row.optionD || '') === '-'
  questionForm.value = {
    type: isTF ? 'TF' : 'SINGLE',
    stem: row.stem || '',
    optionA: row.optionA || '',
    optionB: row.optionB || '',
    optionC: row.optionC || '',
    optionD: row.optionD || '',
    answerIndex: row.answerIndex ?? 0,
    sortNo: row.sortNo ?? 1
  }
  questionDialogVisible.value = true
}

async function saveQuestion() {
  if (!paperQuestionsPaperId.value) return
  if (!String(questionForm.value.stem || '').trim()) {
    ElMessage.warning('请填写题干')
    return
  }

  const type = String(questionForm.value.type || 'SINGLE').toUpperCase()
  if (type === 'TF') {
    const ai = Number(questionForm.value.answerIndex)
    if (!(ai === 0 || ai === 1)) {
      ElMessage.warning('判断题答案只能选“正确/错误”')
      return
    }
  } else {
    const ops = ['optionA', 'optionB', 'optionC', 'optionD']
    for (const k of ops) {
      if (!String(questionForm.value[k] || '').trim()) {
        ElMessage.warning('请填写完整选项 A/B/C/D')
        return
      }
    }
    if (questionForm.value.answerIndex === null || questionForm.value.answerIndex === undefined) {
      ElMessage.warning('请选择正确答案')
      return
    }
  }

  try {
    const isTF = type === 'TF'
    const payload = {
      stem: questionForm.value.stem,
      optionA: isTF ? '正确' : questionForm.value.optionA,
      optionB: isTF ? '错误' : questionForm.value.optionB,
      optionC: isTF ? '-' : questionForm.value.optionC,
      optionD: isTF ? '-' : questionForm.value.optionD,
      answerIndex: questionForm.value.answerIndex,
      sortNo: questionForm.value.sortNo
    }

    const res =
      questionDialogMode.value === 'create'
        ? await apiAdminCreateTestQuestion(paperQuestionsPaperId.value, payload)
        : await apiAdminUpdateTestQuestion(paperQuestionsPaperId.value, editingQuestionId.value, payload)

    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '保存失败')
      return
    }

    ElMessage.success(questionDialogMode.value === 'create' ? '题目已新增' : '题目已更新')
    questionDialogVisible.value = false
    await loadPaperQuestions()
    await loadPapers()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  }
}

async function deleteQuestion(row) {
  if (!paperQuestionsPaperId.value) return
  try {
    const res = await apiAdminDeleteTestQuestion(paperQuestionsPaperId.value, row.id)
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '删除失败')
      return
    }
    ElMessage.success('已删除')
    await loadPaperQuestions()
    await loadPapers()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '删除失败')
  }
}

async function openLessonManager(courseRow) {
  lessonsCourseId.value = courseRow.id
  lessonsCourseTitle.value = courseRow.title || `课程 ${courseRow.id}`
  lessonsDialogVisible.value = true
  await loadLessons()
}

async function loadLessons() {
  if (!lessonsCourseId.value) return
  try {
    const res = await apiAdminLessons(lessonsCourseId.value)
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '加载课时失败')
      return
    }
    lessonRows.value = res.data.data || []
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载课时失败')
  }
}

function openLessonCreate() {
  lessonEditMode.value = 'create'
  editingLessonId.value = null
  lessonForm.value = {
    title: '',
    contentType: 'TEXT',
    contentText: '',
    contentUrl: '',
    durationMin: 10,
    sortNo: 0
  }
  lessonEditDialogVisible.value = true
}

function openLessonEdit(row) {
  lessonEditMode.value = 'edit'
  editingLessonId.value = row.id
  lessonForm.value = {
    title: row.title || '',
    contentType: row.contentType || 'TEXT',
    contentText: row.contentText || '',
    contentUrl: row.contentUrl || '',
    durationMin: row.durationMin ?? 10,
    sortNo: row.sortNo ?? 0
  }
  lessonEditDialogVisible.value = true
}

async function saveLesson() {
  if (!lessonsCourseId.value) return
  if (!lessonForm.value.title.trim()) {
    ElMessage.warning('请填写课时标题')
    return
  }
  if (lessonForm.value.contentType === 'TEXT' && !lessonForm.value.contentText.trim()) {
    ElMessage.warning('请填写文本内容')
    return
  }
  if (lessonForm.value.contentType !== 'TEXT' && !String(lessonForm.value.contentUrl || '').trim()) {
    ElMessage.warning(lessonForm.value.contentType === 'VIDEO' ? '请填写视频地址或链接' : '请填写链接地址')
    return
  }

  try {
    const payload = {
      title: lessonForm.value.title,
      contentType: lessonForm.value.contentType,
      contentText: lessonForm.value.contentType === 'TEXT' ? lessonForm.value.contentText : '',
      contentUrl: lessonForm.value.contentType === 'TEXT' ? '' : lessonForm.value.contentUrl,
      durationMin: lessonForm.value.durationMin,
      sortNo: lessonForm.value.sortNo
    }

    const res =
      lessonEditMode.value === 'create'
        ? await apiAdminCreateLesson(lessonsCourseId.value, payload)
        : await apiAdminUpdateLesson(lessonsCourseId.value, editingLessonId.value, payload)

    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '保存失败')
      return
    }

    ElMessage.success(lessonEditMode.value === 'create' ? '课时已创建' : '课时已更新')
    lessonEditDialogVisible.value = false
    await loadLessons()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  }
}

async function deleteLesson(row) {
  if (!lessonsCourseId.value) return
  try {
    const res = await apiAdminDeleteLesson(lessonsCourseId.value, row.id)
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '删除失败')
      return
    }
    ElMessage.success('已删除')
    await loadLessons()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '删除失败')
  }
}

async function saveRules() {
  try {
    const res = await apiAdminSaveTestRules(ruleSettings.value)
    if (!res.data?.success) return ElMessage.error(res.data?.message || '保存失败')
    ruleSettings.value = res.data.data || ruleSettings.value
    ElMessage.success('规则已保存')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  }
}

async function autoTriage() {
  await loadSharePosts()
  ElMessage.success('队列已刷新')
}

async function bulkReject() {
  try {
    for (const row of moderationQueue.value || []) {
      await apiAdminRejectSharePost(row.id)
    }
    ElMessage.warning('已批量驳回/隐藏')
    await loadSharePosts()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '批量操作失败')
  }
}

async function approvePost(row) {
  try {
    const res = await apiAdminApproveSharePost(row.id)
    if (!res.data?.success) return ElMessage.error(res.data?.message || '操作失败')
    ElMessage.success('已通过/展示')
    await loadSharePosts()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  }
}

async function rejectPost(row) {
  try {
    const res = await apiAdminRejectSharePost(row.id)
    if (!res.data?.success) return ElMessage.error(res.data?.message || '操作失败')
    ElMessage.warning('已驳回/隐藏')
    await loadSharePosts()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  }
}

function viewPost(row) {
  currentPost.value = row
  postDetailVisible.value = true
}

async function resetUser(row) {
  try {
    const res = await apiAdminResetUserPassword(row.id, '123456')
    if (!res.data?.success) return ElMessage.error(res.data?.message || '重置失败')
    ElMessage.success(`已重置密码：${row.username}（新密码：123456）`)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '重置失败')
  }
}

async function toggleUserRole(row) {
  try {
    const next = row.role === 'ADMIN' ? 'USER' : 'ADMIN'
    const res = await apiAdminUpdateUserRole(row.id, next)
    if (!res.data?.success) return ElMessage.error(res.data?.message || '更新失败')
    ElMessage.success('角色已更新')
    await loadUsers()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '更新失败')
  }
}

async function toggleUserStatus(row) {
  try {
    const res = row.status === '停用' ? await apiAdminEnableUser(row.id) : await apiAdminDisableUser(row.id)
    if (!res.data?.success) return ElMessage.error(res.data?.message || '更新失败')
    ElMessage.success('用户状态已更新')
    await loadUsers()
    await loadOverview()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '更新失败')
  }
}


function csvEscape(v) {
  const s = String(v ?? '')
  if (s.includes('"') || s.includes(',') || s.includes('\n') || s.includes('\r')) {
    return `"${s.replaceAll('"', '""')}"`
  }
  return s
}

function downloadText(filename, text) {
  const blob = new Blob([text], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

async function exportReport() {
  exporting.value = true
  try {
    if (!overview.value) await loadOverview()
    if (!reportData.value) await loadReport()

    const now = new Date()
    const ts =
      `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}_` +
      `${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`

    const lines = []
    lines.push('\ufeff运营总览导出')
    lines.push(`导出时间,${csvEscape(now.toLocaleString())}`)
    lines.push('')

    lines.push('总览指标')
    lines.push('指标,数值,备注')
    for (const s of stats.value || []) {
      const note = (s.delta ? `${s.delta} ` : '') + (s.note || '')
      lines.push([csvEscape(s.label), csvEscape(s.value), csvEscape(note.trim())].join(','))
    }

    lines.push('')
    lines.push(`${reportMode.value}（统计报表）`)
    lines.push(`周期,${csvEscape(reportRangeText.value)}`)
    lines.push('指标,数值')
    lines.push(`新增用户,${csvEscape(reportData.value?.newUsers ?? 0)}`)
    lines.push(`新增帖子,${csvEscape(reportData.value?.newPosts ?? 0)}`)
    lines.push(`隐藏帖子,${csvEscape(reportData.value?.hiddenPosts ?? 0)}`)
    lines.push(`测验次数,${csvEscape(reportData.value?.testSubmissions ?? 0)}`)
    lines.push(`活跃学习,${csvEscape(reportData.value?.activeLearners ?? 0)}`)

    lines.push('')
    lines.push('每日明细（仅展示有活动的日期）')
    lines.push('日期,新增用户,新增帖子,隐藏,测验次数,活跃学习')
    for (const r of reportTableRows.value || []) {
      lines.push([
        csvEscape(r.date),
        csvEscape(r.newUsers ?? 0),
        csvEscape(r.newPosts ?? 0),
        csvEscape(r.hiddenPosts ?? 0),
        csvEscape(r.testSubmissions ?? 0),
        csvEscape(r.activeLearners ?? 0)
      ].join(','))
    }

    downloadText(`运营总览_${ts}.csv`, lines.join('\n'))
    ElMessage.success('已导出 CSV')
  } catch (e) {
    ElMessage.error(e?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

async function syncData() {
  await loadCategories()
  await loadCourses()
  await loadPapers()
  await loadRules()
  await loadSharePosts()
  await loadUsers()
  await loadOverview()
  ElMessage.success('数据已刷新')
}

async function switchReport(label) {
  reportMode.value = label
  await loadReport()
}

async function loadReport() {
  reportLoading.value = true
  try {
    const res = reportMode.value === '季报' ? await apiAdminQuarterlyReport() : await apiAdminMonthlyReport()
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '加载报表失败')
      return
    }
    reportData.value = res.data.data || null
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载报表失败')
  } finally {
    reportLoading.value = false
  }
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  const u = localStorage.getItem('user')
  if (u) {
    try {
      me.value = JSON.parse(u)
    } catch {
      me.value = null
    }
  }
  await loadCategories()
  await loadCourses()
  await loadPapers()
  await loadRules()
  await loadSharePosts()
  await loadUsers()
  await loadOverview()
  await loadReport()
  nextTick(() => relayoutTables())
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (resizeTimer) clearTimeout(resizeTimer)
})

async function loadCategories() {
  try {
    const res = await apiCategories()
    if (res.data?.success) {
      courseCategories.value = res.data.data || []
    }
  } catch {
    courseCategories.value = []
  }
}

async function loadCourses() {
  try {
    const res = await apiAdminCourses()
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '加载课程失败')
      return
    }
    const list = res.data.data || []
    courseRows.value = list.map((c) => ({
      id: c.id,
      title: c.title || '',
      categoryId: c.categoryId,
      coverUrl: c.coverUrl || '',
      summary: c.summary || '',
      difficulty: c.difficulty || '中等',
      status: c.enabled != null ? (c.enabled == 1 ? '已发布' : '草稿') : '草稿',
      updated: c.createTime ? String(c.createTime).substring(0, 10) : ''
    }))
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载课程失败')
  }
}

async function loadRules() {
  try {
    const res = await apiAdminGetTestRules()
    if (res.data?.success) {
      ruleSettings.value = res.data.data || ruleSettings.value
    }
  } catch {
    // ignore
  }
}

async function loadSharePosts() {
  try {
    const res = await apiAdminSharePosts()
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '加载帖子审核列表失败')
      return
    }
    const list = res.data.data || []
    moderationQueue.value = list.map((p) => ({
      // 后端返回：statusKey = approved/rejected
      id: p.id,
      title: p.title || '',
      content: p.content || '',
      author: p.author || '匿名',
      risk: p.statusKey === 'rejected' ? '中' : '低',
      status: p.status || (p.statusKey === 'rejected' ? '已驳回/隐藏' : '已通过/展示'),
      statusKey: p.statusKey || 'approved',
      time: p.createdAt ? String(p.createdAt).substring(11, 16) : '',
      tags: p.tags || [],
      commentCount: p.commentCount || 0
    }))
  } catch {
    ElMessage.error('加载帖子审核列表失败：请确认后端 8080 正在运行且已用管理员账号登录')
    moderationQueue.value = []
  }
}

async function loadUsers() {
  try {
    const res = await apiAdminUsers()
    if (!res.data?.success) {
      ElMessage.error(res.data?.message || '加载用户列表失败')
      return
    }
    const list = res.data.data || []
    userRows.value = list.map((u) => ({
      id: u.id,
      username: u.username || '',
      role: u.role || 'USER',
      status: u.disabled ? '停用' : '正常',
      region: u.region || '',
      lastLogin: u.createTime ? String(u.createTime).substring(0, 16).replace('T', ' ') : ''
    }))
  } catch {
    ElMessage.error('加载用户列表失败：请确认后端 8080 正在运行且已用管理员账号登录')
    userRows.value = []
  }
}

async function loadOverview() {
  try {
    const res = await apiAdminOverviewReport()
    if (res.data?.success) overview.value = res.data.data || null
  } catch {
    overview.value = null
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Sans+SC:wght@400;500;600;700&family=Space+Grotesk:wght@400;600;700&display=swap');

:global(:root) {
  --admin-ink: #0f172a;
  --admin-muted: rgba(15, 23, 42, 0.6);
  --admin-soft: rgba(15, 23, 42, 0.08);
  --admin-card: rgba(255, 255, 255, 0.85);
  --admin-stroke: rgba(15, 23, 42, 0.12);
  --admin-accent: #0ea5a4;
  --admin-accent-2: #f59e0b;
  --admin-accent-3: #ef4444;
}

.admin {
  min-height: 100vh;
  padding: 28px clamp(18px, 4vw, 40px) 60px;
  font-family: 'Noto Sans SC', 'Space Grotesk', 'Manrope', sans-serif;
  color: var(--admin-ink);
  background: radial-gradient(1200px 900px at 10% -10%, #dbeafe 0%, transparent 60%),
    radial-gradient(1000px 600px at 90% 10%, #d1fae5 0%, transparent 55%),
    linear-gradient(180deg, #f8fafc 0%, #eff6ff 100%);
  position: relative;
  overflow: hidden;
}

.mesh {
  position: absolute;
  border-radius: 999px;
  filter: blur(60px);
  opacity: 0.35;
  animation: float 10s ease-in-out infinite;
}
.m1 { width: 380px; height: 380px; background: #22c55e; top: -120px; left: -80px; }
.m2 { width: 320px; height: 320px; background: #38bdf8; top: 40px; right: -120px; animation-delay: -2s; }
.m3 { width: 260px; height: 260px; background: #f97316; bottom: -80px; left: 40%; animation-delay: -4s; }

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 22px;
  border-radius: 20px;
  background: var(--admin-card);
  border: 1px solid var(--admin-stroke);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.12);
  position: relative;
  z-index: 2;
  animation: rise 0.6s ease both;
}

.brand { display: flex; align-items: center; gap: 14px; }
.brandBadge {
  width: 54px; height: 54px; border-radius: 16px;
  display: grid; place-items: center;
  font-weight: 700; color: #fff;
  background: linear-gradient(135deg, #0ea5a4, #22c55e);
  box-shadow: 0 10px 30px rgba(14, 165, 164, 0.35);
}
.brandTitle { font-size: 20px; font-weight: 700; }
.brandSub { color: var(--admin-muted); font-size: 13px; }

.topActions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.chip {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(14, 165, 164, 0.12);
  color: #0f766e;
  font-weight: 600;
  font-size: 12px;
}
.primaryBtn { border-radius: 999px; padding: 8px 16px; }

.shell { display: grid; grid-template-columns: 260px 1fr; gap: 20px; margin-top: 20px; position: relative; z-index: 2; }

.side {
  position: sticky;
  top: 20px;
  align-self: start;
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--admin-stroke);
  backdrop-filter: blur(10px);
}
.sideTitle { font-size: 14px; font-weight: 700; margin-bottom: 12px; }
.sideItem {
  width: 100%;
  text-align: left;
  border: 1px solid transparent;
  background: transparent;
  padding: 10px 12px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 8px;
}
.sideItem:hover { background: rgba(15, 23, 42, 0.06); }
.sideItem.active {
  background: rgba(14, 165, 164, 0.12);
  border-color: rgba(14, 165, 164, 0.35);
}
.sideItemTitle { font-weight: 600; }
.sideItemSub { color: var(--admin-muted); font-size: 12px; }

.sideNote {
  margin-top: 16px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.04);
}
.sideNoteTitle { font-weight: 600; margin-bottom: 8px; }
.sideNoteRow { display: flex; justify-content: space-between; font-size: 13px; color: var(--admin-muted); }
.sideNoteRow strong { color: var(--admin-ink); }

.content { display: flex; flex-direction: column; gap: 22px; min-width: 0; }
.section { scroll-margin-top: 120px; }
.sectionHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}
.sectionTitle { font-size: 18px; font-weight: 700; }
.sectionSub { color: var(--admin-muted); font-size: 13px; }
.sectionActions { display: flex; flex-wrap: wrap; gap: 10px; }

.statGrid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}
.statCard {
  padding: 16px;
  border-radius: 16px;
  background: var(--admin-card);
  border: 1px solid var(--admin-stroke);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
  animation: rise 0.6s ease both;
  animation-delay: calc(var(--i) * 80ms);
}
.statLabel { color: var(--admin-muted); font-size: 12px; letter-spacing: 0.2px; }
.statValue { font-size: 26px; font-weight: 700; margin: 6px 0; }
.statMeta { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.statDelta { font-weight: 600; }
.statDelta.up { color: #16a34a; }
.statDelta.down { color: #ef4444; }
.statNote { color: var(--admin-muted); }

.panel {
  min-width: 0;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid var(--admin-stroke);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.table { width: 100%; }
:deep(.el-table__body-wrapper) { overflow-x: auto; }
.postDetailWrap { display: grid; gap: 12px; }
.postDetailTitle { font-size: 20px; font-weight: 700; line-height: 1.4; }
.postDetailMeta { display: flex; flex-wrap: wrap; gap: 12px; color: var(--admin-muted); font-size: 12px; }
.postDetailTags { display: flex; flex-wrap: wrap; gap: 8px; }
.postDetailContent { white-space: pre-wrap; line-height: 1.9; color: rgba(15,23,42,.82); background: rgba(14,165,164,0.06); border: 1px solid rgba(14,165,164,0.2); border-radius: 12px; padding: 12px; max-height: 52vh; overflow: auto; }

.selectSlim { min-width: 140px; }

.ruleGrid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}
.ruleCard {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--admin-stroke);
  background: rgba(15, 23, 42, 0.03);
}
.ruleTitle { font-weight: 700; margin-bottom: 10px; }
.ruleRow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
  color: var(--admin-muted);
}
.ruleMetric {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}
.ruleLabel { color: var(--admin-muted); font-size: 12px; }
.ruleValue { font-weight: 600; }

.mixRow { display: grid; grid-template-columns: 80px 1fr 120px; align-items: center; gap: 10px; margin-bottom: 8px; }
.mixLabel { color: var(--admin-muted); font-size: 12px; }
.mixBar {
  height: 8px;
  background: rgba(15, 23, 42, 0.08);
  border-radius: 999px;
  overflow: hidden;
}
.mixBar span { display: block; height: 100%; border-radius: 999px; }
.mixValue { font-size: 12px; font-weight: 600; text-align: right; }
.mixHint { margin-top: 6px; font-size: 12px; color: var(--admin-muted); }
.mixHint.bad { color: #b91c1c; font-weight: 600; }

.pill {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(15, 23, 42, 0.08);
}
.pill.review { background: rgba(14, 165, 164, 0.15); color: #0f766e; }
.pill.queued { background: rgba(59, 130, 246, 0.15); color: #1d4ed8; }
.pill.flagged { background: rgba(239, 68, 68, 0.2); color: #b91c1c; }
.pill.approved { background: rgba(34, 197, 94, 0.2); color: #15803d; }
.pill.rejected { background: rgba(239, 68, 68, 0.18); color: #b91c1c; }
.pill.soft { background: rgba(15, 23, 42, 0.06); }

.reportGrid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}
.reportCard {
  padding: 16px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.03);
  border: 1px solid var(--admin-stroke);
}
.reportTitle { font-weight: 700; }
.reportValue { font-size: 26px; font-weight: 700; margin: 6px 0 10px; }
.reportNote { color: var(--admin-muted); font-size: 12px; margin-bottom: 10px; }
.reportBars { display: grid; gap: 8px; }
.barRow { display: grid; grid-template-columns: 40px 1fr 50px; gap: 8px; align-items: center; font-size: 12px; }
.bar {
  height: 8px;
  background: rgba(15, 23, 42, 0.08);
  border-radius: 999px;
  overflow: hidden;
}
.bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #0ea5a4, #22c55e);
  animation: grow 0.8s ease both;
  animation-delay: calc(var(--i) * 120ms);
}
.pillGroup { display: flex; flex-wrap: wrap; gap: 6px; }
.signalRow { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 10px; }
.signal {
  padding: 10px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.04);
  text-align: center;
  font-size: 12px;
  color: var(--admin-muted);
}
.signal strong { display: block; color: var(--admin-ink); font-size: 16px; margin-top: 4px; }

:deep(.el-input__wrapper) { border-radius: 12px; }
:deep(.el-select .el-input__wrapper) { border-radius: 12px; }
:deep(.el-table) { background: transparent; }
:deep(.el-table th.el-table__cell) { background: rgba(15, 23, 42, 0.04); }
:deep(.el-table tr) { background: transparent; }

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(12px); }
}

@keyframes rise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes grow {
  from { transform: scaleX(0.4); transform-origin: left; opacity: 0.6; }
  to { transform: scaleX(1); opacity: 1; }
}

@media (max-width: 1100px) {
  .shell { grid-template-columns: 1fr; }
  .side { position: relative; top: 0; }
}

@media (max-width: 720px) {
  .topbar { flex-direction: column; align-items: flex-start; }
  .sectionHeader { flex-direction: column; align-items: flex-start; }
  .sectionActions { width: 100%; }
}

@media (prefers-reduced-motion: reduce) {
  .mesh, .statCard, .bar i { animation: none !important; }
}
</style>
