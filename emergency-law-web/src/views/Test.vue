<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand" @click="goLearn">
        <div class="logo">EL</div>
        <div>
          <div class="brandTitle">在线测试</div>
          <div class="brandSub">检验学习成果 · 自动统计成绩</div>
        </div>
      </div>

      <div class="right">
        <el-button text @click="goLearn" class="navBtn">学习</el-button>
        <el-button text @click="goTest" class="navBtn active">在线测试</el-button>
        <el-button text @click="goHistory" class="navBtn">我的成绩</el-button>
        <el-button text @click="goShare" class="navBtn">经验分享</el-button>

        <el-tag v-if="me?.role" effect="light">{{ me.role }}</el-tag>
        <div class="user">
          <div class="userName">{{ me?.nickname || me?.username || '未登录' }}</div>
          <div class="userSub">可答辩演示</div>
        </div>

        <el-button plain @click="logout">退出</el-button>
      </div>
    </header>

    <div class="wrap">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索试卷标题..." clearable style="max-width: 420px" />
        <div class="mutedSmall">共 {{ filtered.length }} 套试卷</div>
      </div>

      <div v-if="filtered.length === 0" class="emptyCard">
        <div class="emptyTitle">暂无试卷</div>
        <div class="emptyMuted">后端接口未接入时，会使用本地演示题库</div>
      </div>

      <div v-else class="grid">
        <div v-for="p in filtered" :key="p.id" class="card">
          <div class="cardTop">
            <div class="title">{{ p.title }}</div>
            <el-tag size="small" effect="light">{{ p.difficulty }}</el-tag>
          </div>

          <div class="meta">
            <div class="metaItem">题数：{{ p.questionCount }}</div>
            <div class="metaItem">时长：{{ p.durationMin }} 分钟</div>
          </div>

          <div class="tags">
            <el-tag v-for="t in (p.tags || [])" :key="t" size="small" effect="light" class="tagEl">{{ t }}</el-tag>
          </div>

          <div class="actions">
            <el-button @click="start(p.id)">开始测试</el-button>
            <el-button type="primary" plain @click="preview(p.id)">预览题目</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="previewOpen" title="试卷预览" width="760px">
      <div v-if="previewPaper" class="pv">
        <div class="pvTitle">{{ previewPaper.title }}</div>
        <div class="pvSub">共 {{ previewPaper.questions?.length || 0 }} 题 · {{ previewPaper.durationMin }} 分钟</div>

        <div class="pvList">
          <div v-for="(q, idx) in (previewPaper.questions || [])" :key="q.id" class="pvQ">
            <div class="qStem">{{ idx + 1 }}. {{ q.stem }}</div>
            <div class="qOps">
              <div v-for="(op, i) in q.options" :key="i" class="op" v-if="op && op !== '-'">{{ String.fromCharCode(65+i) }}. {{ op }}</div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="previewOpen = false">关闭</el-button>
        <el-button type="primary" @click="start(previewPaper?.id)">开始测试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiPaperDetail, apiPaperList, demoLoadPaperDetail, demoLoadPapers } from '../api/test'

const router = useRouter()

const me = ref(null)
const papers = ref([])
const keyword = ref('')

const previewOpen = ref(false)
const previewPaper = ref(null)

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.location.href = '/login'
}

function goLearn() { router.push('/learn') }
function goTest() { router.push('/test') }
function goHistory() { router.push('/test/history') }
function goShare() { router.push('/share') }

function loadMe() {
  try {
    const u = localStorage.getItem('user')
    me.value = u ? JSON.parse(u) : null
  } catch {
    me.value = null
  }
}

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return papers.value
  return papers.value.filter(p => (p.title || '').toLowerCase().includes(k))
})

async function loadList() {
  // 先尝试后端
  try {
    const res = await apiPaperList()
    if (res.data?.success) {
      papers.value = res.data.data || []
      return
    }
    throw new Error(res.data?.message || 'api not ready')
  } catch {
    // 本地演示
    papers.value = demoLoadPapers().map(p => ({
      id: p.id,
      title: p.title,
      difficulty: p.difficulty,
      durationMin: p.durationMin,
      questionCount: p.questionCount,
      tags: p.tags
    }))
  }
}

async function preview(id) {
  try {
    const res = await apiPaperDetail(id)
    if (res.data?.success) {
      previewPaper.value = res.data.data
    } else {
      throw new Error('api not ready')
    }
  } catch {
    previewPaper.value = demoLoadPaperDetail(id)
  }
  previewOpen.value = true
}

function start(id) {
  if (!id) return
  router.push(`/test/take/${id}`)
}

onMounted(async () => {
  loadMe()
  await loadList()
  ElMessage.success('在线测试模块已就绪')
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

.right { display: flex; align-items: center; gap: 10px; }
.navBtn { font-weight: 800; }
.navBtn.active { color: #2563eb; }

.user { text-align: right; }
.userName { font-weight: 800; }
.userSub { font-size: 12px; color: var(--muted); }

.wrap { padding: 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }

.card {
  background: var(--card); border: 1px solid var(--border); border-radius: 16px;
  box-shadow: 0 18px 55px rgba(15, 23, 42, 0.06);
  padding: 14px; display: grid; gap: 10px;
}
.cardTop { display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; }
.title { font-weight: 900; line-height: 1.25; }

.meta { display: flex; gap: 12px; color: rgba(15,23,42,0.65); font-size: 12px; }
.tags { display: flex; flex-wrap: wrap; gap: 8px; }
.tagEl { border-radius: 999px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }

.emptyCard { padding: 18px; border-radius: 16px; border: 1px dashed rgba(15, 23, 42, 0.14); background: rgba(255, 255, 255, 0.6); }
.emptyTitle { font-weight: 900; margin-bottom: 6px; }
.emptyMuted, .mutedSmall { color: var(--muted); font-size: 12px; }

.pvTitle { font-weight: 900; }
.pvSub { margin-top: 6px; color: var(--muted); font-size: 12px; }
.pvList { margin-top: 12px; display: grid; gap: 12px; }
.pvQ { padding: 12px; border-radius: 14px; border: 1px solid rgba(15,23,42,0.08); background: rgba(255,255,255,0.7); }
.qStem { font-weight: 900; margin-bottom: 8px; }
.qOps { display: grid; gap: 6px; color: rgba(15,23,42,0.78); }
.op { font-size: 12px; }
</style>
