<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand" @click="goTest">
        <div class="backBtn">←</div>
        <div>
          <div class="brandTitle">做题中</div>
          <div class="brandSub">{{ paper?.title || '加载中...' }}</div>
        </div>
      </div>

      <div class="right">
        <div class="timerBox">
          <div class="timerTitle">{{ timeLimitMin ? '剩余' : '用时' }}</div>
          <div class="timerValue">{{ timeLimitMin ? remainingText : (usedMin + ' min') }}</div>
        </div>

        <el-button text @click="goTest" class="navBtn">返回试卷</el-button>
        <el-button text @click="goHistory" class="navBtn">我的成绩</el-button>
        <el-button type="primary" class="pill" :loading="submitting" @click="submit">交卷</el-button>
      </div>
    </header>

    <div class="wrap">
      <aside class="side">
        <div class="panel">
          <div class="panelTitle">答题进度</div>
          <el-progress :percentage="progressPct" :stroke-width="10" />
          <div class="mutedSmall">{{ answeredCount }} / {{ total }} 已作答</div>
        </div>

        <div class="panel">
          <div class="panelTitle">题号</div>
          <div class="numGrid">
            <div
                v-for="(q, idx) in questions"
                :key="q.id"
                class="num"
                :class="{ active: idx === currentIndex, done: answers[idx] !== null }"
                @click="currentIndex = idx"
            >
              {{ idx + 1 }}
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panelTitle">提示</div>
          <div class="mutedSmall">
            单选题：选择后自动保存到本页内存。<br/>
            点击右上角“交卷”提交到后端并写入成绩记录。
          </div>
        </div>
      </aside>

      <section class="main">
        <div v-if="loading" class="emptyCard">
          <div class="emptyTitle">加载试卷中...</div>
          <div class="emptyMuted">请稍等</div>
        </div>

        <div v-else-if="questions.length === 0" class="emptyCard">
          <div class="emptyTitle">这套试卷没有题目</div>
          <div class="emptyMuted">请去数据库给 test_question 插入题目</div>
        </div>

        <div v-else class="panel big">
          <div class="qHead">
            <div class="qIndex">第 {{ currentIndex + 1 }} 题 / {{ total }} 题</div>
            <el-tag size="small" effect="light">{{ paper?.difficulty || 'EASY' }}</el-tag>
          </div>

          <div class="qStem">{{ currentQ?.stem }}</div>

          <el-radio-group v-model="answers[currentIndex]" class="optGroup" :disabled="isTimeUp || submitting || resultOpen">
            <el-radio :label="0" class="opt">{{ currentQ?.options?.[0] }}</el-radio>
            <el-radio :label="1" class="opt">{{ currentQ?.options?.[1] }}</el-radio>
            <el-radio v-if="currentQ?.options?.[2] && currentQ.options[2] !== '-'" :label="2" class="opt">{{ currentQ?.options?.[2] }}</el-radio>
            <el-radio v-if="currentQ?.options?.[3] && currentQ.options[3] !== '-'" :label="3" class="opt">{{ currentQ?.options?.[3] }}</el-radio>
          </el-radio-group>

          <div class="navRow">
            <el-button :disabled="currentIndex === 0" @click="currentIndex--">上一题</el-button>
            <el-button :disabled="currentIndex === total - 1" @click="currentIndex++">下一题</el-button>
          </div>
        </div>
      </section>
    </div>

    <el-dialog v-model="resultOpen" title="测试结果" width="520px">
      <div class="resultBox">
        <div class="score">{{ result?.score ?? '-' }}</div>
        <div class="resultMeta">
          <div>正确：{{ result?.correctCount ?? '-' }} / {{ result?.total ?? '-' }}</div>
          <div>用时：{{ result?.usedMin ?? usedMin }} min</div>
          <div>及格线：{{ passScore }} 分 · {{ passed === null ? '-' : (passed ? '已及格' : '未及格') }}</div>
          <div>记录ID：{{ result?.recordId ?? '-' }}</div>
        </div>
      </div>

      <template #footer>
        <el-button @click="goTest">返回试卷</el-button>
        <el-button type="primary" @click="goHistory">查看成绩</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiTestPaperDetail, apiTestSubmit, apiTestRules } from '../api/test'

const route = useRoute()
const router = useRouter()

const paperId = Number(route.params.paperId)

const loading = ref(true)
const submitting = ref(false)
const paper = ref(null)
const questions = ref([])
const answers = ref([]) // 每题的选择，null表示未答
const currentIndex = ref(0)

const usedSec = ref(0)
let timer = null
const timeLimitMin = ref(null)
const autoSubmitted = ref(false)

const resultOpen = ref(false)
const result = ref(null)
const rules = ref(null)

const total = computed(() => questions.value.length)
const currentQ = computed(() => questions.value[currentIndex.value])

const answeredCount = computed(() => answers.value.filter(x => x !== null && x !== undefined).length)
const progressPct = computed(() => total.value === 0 ? 0 : Math.round(answeredCount.value * 100 / total.value))
const usedMin = computed(() => Math.max(1, Math.ceil(usedSec.value / 60)))
const remainingSec = computed(() => {
  const m = timeLimitMin.value
  if (!m || m <= 0) return null
  return Math.max(0, m * 60 - usedSec.value)
})
const remainingText = computed(() => {
  const s = remainingSec.value
  if (s === null) return ''
  const mm = String(Math.floor(s / 60)).padStart(2, '0')
  const ss = String(s % 60).padStart(2, '0')
  return `${mm}:${ss}`
})
const isTimeUp = computed(() => remainingSec.value !== null && remainingSec.value <= 0)
const passScore = computed(() => rules.value?.passScore ?? 80)
const passed = computed(() => {
  const s = result.value?.score
  if (s === null || s === undefined) return null
  return Number(s) >= Number(passScore.value)
})

function goTest() { router.push('/test') }
function goHistory() { router.push('/test/history') }

async function loadPaper() {
  loading.value = true
  try {
    const res = await apiTestPaperDetail(paperId)
    if (!res.data?.success) throw new Error(res.data?.message || 'load failed')
    paper.value = res.data.data
    questions.value = res.data.data?.questions || []
    answers.value = questions.value.map(() => null)
    currentIndex.value = 0
  } catch (e) {
    ElMessage.error('加载试卷失败：请确认后端 8080 正常运行')
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (total.value === 0) return
  if (submitting.value) return
  submitting.value = true
  try {
    const payload = {
      paperId,
      questionIds: (questions.value || []).map(q => q.id),
      answers: answers.value.map(x => (x === null || x === undefined) ? -1 : x),
      usedMin: usedMin.value
    }
    // 后端按 null 处理“未答”，我们这里用 -1 也可以（会算错，不会报错）
    const res = await apiTestSubmit(payload)
    if (!res.data?.success) throw new Error(res.data?.message || 'submit failed')
    result.value = res.data.data
    resultOpen.value = true
    ElMessage.success('交卷成功')
  } catch (e) {
    ElMessage.error('交卷失败：' + (e?.message || '请重试'))
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const rr = await apiTestRules()
    if (rr.data?.success) {
      rules.value = rr.data.data
      const tl = rr.data.data?.timeLimit
      timeLimitMin.value = (typeof tl === 'number' && tl > 0) ? tl : null
    }
  } catch {
    rules.value = null
    timeLimitMin.value = null
  }
  await loadPaper()
  timer = setInterval(async () => {
    const limitSec = timeLimitMin.value ? timeLimitMin.value * 60 : null
    if (limitSec !== null) usedSec.value = Math.min(usedSec.value + 1, limitSec)
    else usedSec.value++

    if (!timeLimitMin.value) return
    if (autoSubmitted.value) return
    if (remainingSec.value !== null && remainingSec.value <= 0) {
      autoSubmitted.value = true
      ElMessage.warning('已到限时，系统自动交卷')
      await submit()
    }
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.shell { min-height: 100vh; background: var(--bg); }
.topbar {
  height: 66px; padding: 0 18px; display: flex; align-items: center; justify-content: space-between;
  background: rgba(255, 255, 255, 0.88); border-bottom: 1px solid var(--border);
  backdrop-filter: blur(10px); position: sticky; top: 0; z-index: 10;
}
.brand { display: flex; align-items: center; gap: 12px; cursor: pointer; }
.backBtn {
  width: 36px; height: 36px; border-radius: 12px; display: grid; place-items: center;
  background: rgba(15, 23, 42, 0.04); border: 1px solid rgba(15, 23, 42, 0.08);
  font-weight: 900;
}
.brandTitle { font-weight: 900; }
.brandSub { font-size: 12px; color: var(--muted); margin-top: 2px; }

.right { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.navBtn { font-weight: 800; }
.pill { border-radius: 999px; font-weight: 900; }

.wrap { display: grid; grid-template-columns: 320px 1fr; gap: 16px; padding: 16px; }
.side { position: sticky; top: 88px; height: calc(100vh - 104px); overflow: auto; display: grid; gap: 14px; }

.panel {
  background: var(--card); border: 1px solid var(--border); border-radius: 16px;
  box-shadow: 0 20px 55px rgba(15, 23, 42, 0.06); padding: 14px;
}
.panelTitle { font-weight: 900; margin-bottom: 10px; }

.numGrid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 8px; }
.num {
  height: 34px; border-radius: 12px; display: grid; place-items: center;
  font-weight: 900; cursor: pointer; border: 1px solid rgba(15,23,42,0.08);
  background: rgba(15,23,42,0.02);
}
.num.done { background: linear-gradient(135deg, rgba(34,197,94,0.16), rgba(59,130,246,0.10)); }
.num.active { outline: 2px solid rgba(37,99,235,0.35); }

.big { min-height: 420px; }
.qHead { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.qIndex { font-weight: 900; color: rgba(15,23,42,0.75); font-size: 12px; }
.qStem { margin-top: 12px; font-weight: 900; line-height: 1.7; }
.optGroup { margin-top: 14px; display: grid; gap: 10px; }
.opt {
  padding: 12px 12px; border-radius: 14px;
  border: 1px solid rgba(15,23,42,0.08);
  background: rgba(255,255,255,0.7);
}
.navRow { margin-top: 16px; display: flex; justify-content: space-between; gap: 10px; }

.timerBox { text-align: right; padding: 6px 10px; border-radius: 14px; border: 1px solid rgba(15,23,42,0.08); background: rgba(255,255,255,0.7); }
.timerTitle { font-size: 12px; color: var(--muted); font-weight: 900; }
.timerValue { font-weight: 900; }

.resultBox { display: grid; gap: 10px; place-items: center; padding: 8px 0; }
.score { font-size: 54px; font-weight: 900; }
.resultMeta { color: rgba(15,23,42,0.7); display: grid; gap: 6px; text-align: center; }

.emptyCard { padding: 18px; border-radius: 16px; border: 1px dashed rgba(15, 23, 42, 0.14); background: rgba(255, 255, 255, 0.6); }
.emptyTitle { font-weight: 900; margin-bottom: 6px; }
.emptyMuted, .mutedSmall { color: var(--muted); font-size: 12px; }
</style>
