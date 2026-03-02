<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand" @click="goLearn">
        <div class="logo">EL</div>
        <div>
          <div class="brandTitle">学习统计</div>
          <div class="brandSub">数据采集与分析 · 答辩亮点</div>
        </div>
      </div>

      <div class="right">
        <el-button text @click="goLearn" class="navBtn">学习</el-button>
        <el-button text @click="goTest" class="navBtn">在线测试</el-button>
        <el-button text @click="goHistory" class="navBtn">我的成绩</el-button>
        <el-button text @click="goStats" class="navBtn active">学习统计</el-button>
        <el-button text @click="goShare" class="navBtn">经验分享</el-button>

        <el-tag v-if="me?.role" effect="light">{{ me.role }}</el-tag>
        <div class="user">
          <div class="userName">{{ me?.nickname || me?.username || '未登录' }}</div>
          <div class="userSub">来源：study_progress 表</div>
        </div>
        <el-button plain @click="logout">退出</el-button>
      </div>
    </header>

    <div class="wrap">
      <div class="cards">
        <div class="kpiCard">
          <div class="kpiTitle">学习课程数</div>
          <div class="kpiValue">{{ kpiCourses }}</div>
          <div class="kpiSub">有进度记录的课程数量</div>
        </div>

        <div class="kpiCard">
          <div class="kpiTitle">平均进度</div>
          <div class="kpiValue">{{ kpiAvg }}%</div>
          <div class="kpiSub">对所有学习课程取平均</div>
        </div>

        <div class="kpiCard">
          <div class="kpiTitle">最高进度</div>
          <div class="kpiValue">{{ kpiMax }}%</div>
          <div class="kpiSub">{{ kpiMaxCourse }}</div>
        </div>

        <div class="kpiCard">
          <div class="kpiTitle">最近学习时间</div>
          <div class="kpiValue small">{{ kpiLatestTime }}</div>
          <div class="kpiSub">{{ kpiLatestCourse }}</div>
        </div>
      </div>

      <div class="grid">
        <div class="panel">
          <div class="panelTitle">课程进度分布</div>
          <div ref="pieRef" class="chart"></div>
        </div>

        <div class="panel">
          <div class="panelTitle">课程进度排行（Top 10）</div>
          <div ref="barRef" class="chart"></div>
        </div>
      </div>

      <div class="panel">
        <div class="panelTitle">学习记录列表</div>
        <el-table :data="tableRows" stripe style="width: 100%">
          <el-table-column prop="courseTitle" label="课程" min-width="220" />
          <el-table-column prop="progressPercent" label="进度" width="90" />
          <el-table-column prop="status" label="状态" width="110" />
          <el-table-column prop="lessonId" label="课时ID" width="90" />
          <el-table-column prop="lastStudyTime" label="最近学习时间" min-width="190" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="goCourse(row.courseId)">进入</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onBeforeUnmount, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { apiMyProgressList, apiCourses } from '../api/learn'

const router = useRouter()

const me = ref(null)
const progressList = ref([])
const courses = ref([])

const pieRef = ref(null)
const barRef = ref(null)
let pieChart = null
let barChart = null

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.location.href = '/login'
}
function goLearn() { router.push('/learn') }
function goStats() { router.push('/stats') }
function goShare() { router.push('/share') }
function goCourse(courseId) { router.push(`/learn/course/${courseId}`) }
function goTest() { router.push('/test') }
function goHistory() { router.push('/test/history') }

function loadMe() {
  try {
    const u = localStorage.getItem('user')
    me.value = u ? JSON.parse(u) : null
  } catch {
    me.value = null
  }
}

const courseTitleMap = computed(() => {
  const m = {}
  for (const c of courses.value) m[String(c.id)] = c.title
  return m
})

const tableRows = computed(() => {
  return (progressList.value || []).map(p => ({
    ...p,
    courseTitle: courseTitleMap.value[String(p.courseId)] || `课程 ${p.courseId}`
  }))
})

const kpiCourses = computed(() => (progressList.value || []).length)

const kpiAvg = computed(() => {
  const arr = progressList.value || []
  if (arr.length === 0) return 0
  const sum = arr.reduce((s, x) => s + (x.progressPercent || 0), 0)
  return Math.round(sum / arr.length)
})

const maxItem = computed(() => {
  const arr = progressList.value || []
  if (arr.length === 0) return null
  return arr.reduce((a, b) => ((a.progressPercent || 0) >= (b.progressPercent || 0) ? a : b))
})

const kpiMax = computed(() => maxItem.value?.progressPercent ?? 0)
const kpiMaxCourse = computed(() => {
  if (!maxItem.value) return '暂无'
  return courseTitleMap.value[String(maxItem.value.courseId)] || `课程 ${maxItem.value.courseId}`
})

const latestItem = computed(() => {
  const arr = progressList.value || []
  if (arr.length === 0) return null
  return arr.reduce((a, b) => (String(a.lastStudyTime) >= String(b.lastStudyTime) ? a : b))
})

const kpiLatestTime = computed(() => latestItem.value?.lastStudyTime ?? '暂无')
const kpiLatestCourse = computed(() => {
  if (!latestItem.value) return '暂无'
  return courseTitleMap.value[String(latestItem.value.courseId)] || `课程 ${latestItem.value.courseId}`
})

function buildPieData() {
  const buckets = [
    { name: '0-20%', value: 0 },
    { name: '20-50%', value: 0 },
    { name: '50-80%', value: 0 },
    { name: '80-100%', value: 0 }
  ]
  for (const p of progressList.value || []) {
    const v = p.progressPercent || 0
    if (v < 20) buckets[0].value++
    else if (v < 50) buckets[1].value++
    else if (v < 80) buckets[2].value++
    else buckets[3].value++
  }
  return buckets
}

function buildBarData() {
  const arr = (progressList.value || [])
      .map(p => ({
        courseId: p.courseId,
        name: courseTitleMap.value[String(p.courseId)] || `课程 ${p.courseId}`,
        value: p.progressPercent || 0
      }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 10)

  return { names: arr.map(x => x.name), values: arr.map(x => x.value) }
}

async function renderCharts() {
  await nextTick()

  if (pieRef.value) {
    pieChart?.dispose()
    pieChart = echarts.init(pieRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      series: [
        { type: 'pie', radius: ['35%', '70%'], data: buildPieData(), label: { formatter: '{b}\n{c}' } }
      ]
    })
  }

  if (barRef.value) {
    barChart?.dispose()
    barChart = echarts.init(barRef.value)
    const { names, values } = buildBarData()
    barChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 20, bottom: 90 },
      xAxis: { type: 'category', data: names, axisLabel: { rotate: 30 } },
      yAxis: { type: 'value', max: 100 },
      series: [{ type: 'bar', data: values }]
    })
  }
}

function onResize() {
  pieChart?.resize()
  barChart?.resize()
}

async function loadData() {
  const cr = await apiCourses(null)
  if (cr.data?.success) courses.value = cr.data.data || []

  const pr = await apiMyProgressList()
  if (!pr.data?.success) throw new Error(pr.data?.message || 'load progress failed')
  progressList.value = pr.data.data || []
}

onMounted(async () => {
  loadMe()
  try {
    await loadData()
    await renderCharts()
    window.addEventListener('resize', onResize)
  } catch (e) {
    ElMessage.error('统计加载失败：请确认后端 8080 正在运行')
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  pieChart?.dispose()
  barChart?.dispose()
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
.logo {
  width: 38px; height: 38px; border-radius: 14px; display: grid; place-items: center;
  font-weight: 900; color: #fff; background: linear-gradient(135deg, #3b82f6, #22c55e);
  box-shadow: 0 14px 30px rgba(59, 130, 246, 0.22);
}
.brandTitle { font-weight: 900; }
.brandSub { font-size: 12px; color: var(--muted); margin-top: 2px; }

.right { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.navBtn { font-weight: 800; }
.navBtn.active { color: #2563eb; }

.user { text-align: right; }
.userName { font-weight: 800; }
.userSub { font-size: 12px; color: var(--muted); }

.wrap { padding: 16px; display: grid; gap: 14px; }
.cards { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.kpiCard {
  background: var(--card); border: 1px solid var(--border); border-radius: 16px;
  box-shadow: 0 20px 55px rgba(15, 23, 42, 0.06); padding: 14px;
}
.kpiTitle { font-weight: 900; color: rgba(15,23,42,0.7); font-size: 12px; }
.kpiValue { margin-top: 10px; font-weight: 900; font-size: 28px; }
.kpiValue.small { font-size: 16px; }
.kpiSub { margin-top: 6px; color: var(--muted); font-size: 12px; }

.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.panel {
  background: var(--card); border: 1px solid var(--border); border-radius: 16px;
  box-shadow: 0 20px 55px rgba(15, 23, 42, 0.06); padding: 14px;
}
.panelTitle { font-weight: 900; margin-bottom: 10px; }
.chart { height: 320px; width: 100%; }
</style>
