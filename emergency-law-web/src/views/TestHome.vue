<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand" @click="goLearn">
        <div class="logo">EL</div>
        <div>
          <div class="brandTitle">在线测试</div>
          <div class="brandSub">做题检验学习效果 · 自动保存成绩</div>
        </div>
      </div>

      <div class="right">
        <el-button text @click="goLearn" class="navBtn">学习</el-button>
        <el-button text @click="goTest" class="navBtn active">在线测试</el-button>
        <el-button text @click="goHistory" class="navBtn">我的成绩</el-button>
        <el-button text @click="goStats" class="navBtn">学习统计</el-button>
        <el-button text @click="goShare" class="navBtn">经验分享</el-button>

        <el-tag v-if="me?.role" effect="light">{{ me.role }}</el-tag>
        <div class="user">
          <div class="userName">{{ me?.nickname || me?.username || '未登录' }}</div>
          <div class="userSub">{{ papers.length }} 套试卷</div>
        </div>

        <el-button plain @click="logout">退出</el-button>
      </div>
    </header>

    <div class="wrap">
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索试卷标题..." style="max-width: 420px" />
        <div class="mutedSmall">数据来源：test_paper / test_question</div>
      </div>

      <div v-if="filtered.length === 0" class="emptyCard">
        <div class="emptyTitle">暂无试卷</div>
        <div class="emptyMuted">请确认你已执行 SQL 初始化数据，或在数据库中新增试卷</div>
      </div>

      <div v-else class="grid">
        <div v-for="p in filtered" :key="p.id" class="card">
          <div class="cardHead">
            <div class="title">{{ p.title }}</div>
            <el-tag size="small" effect="light">{{ p.difficulty }}</el-tag>
          </div>

          <div class="metaRow">
            <div class="metaItem">题量：<b>{{ p.questionCount }}</b></div>
            <div class="metaItem">时长：<b>{{ p.durationMin }}</b> min</div>
          </div>

          <div class="tags">
            <el-tag v-for="t in (p.tags || [])" :key="t" size="small" effect="light" class="tagEl">{{ t }}</el-tag>
          </div>

          <div class="actions">
            <el-button @click="start(p.id)">开始测试</el-button>
            <el-button type="primary" @click="start(p.id)">进入做题</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiTestPapers } from '../api/test'

const router = useRouter()
const me = ref(null)
const papers = ref([])
const keyword = ref('')

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.location.href = '/login'
}
function goLearn() { router.push('/learn') }
function goTest() { router.push('/test') }
function goHistory() { router.push('/test/history') }
function goStats() { router.push('/stats') }
function goShare() { router.push('/share') }

function start(id) {
  router.push(`/test/take/${id}`)
}

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return papers.value
  return papers.value.filter(p => (p.title || '').toLowerCase().includes(k))
})

onMounted(async () => {
  try {
    me.value = JSON.parse(localStorage.getItem('user') || 'null')
  } catch { me.value = null }

  try {
    const res = await apiTestPapers()
    if (!res.data?.success) throw new Error(res.data?.message || 'load failed')
    papers.value = res.data.data || []
  } catch (e) {
    ElMessage.error('加载试卷失败：请确认后端 8080 正常运行')
  }
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
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }

.card {
  background: var(--card); border: 1px solid var(--border); border-radius: 16px;
  box-shadow: 0 18px 55px rgba(15, 23, 42, 0.06);
  padding: 14px; display: grid; gap: 10px;
}
.cardHead { display: flex; justify-content: space-between; gap: 10px; align-items: flex-start; }
.title { font-weight: 900; line-height: 1.25; }
.metaRow { display: flex; gap: 12px; color: rgba(15,23,42,0.7); font-size: 12px; }
.tags { display: flex; flex-wrap: wrap; gap: 8px; min-height: 24px; }
.tagEl { border-radius: 999px; }
.actions { display: flex; justify-content: space-between; gap: 10px; }

.emptyCard { padding: 18px; border-radius: 16px; border: 1px dashed rgba(15, 23, 42, 0.14); background: rgba(255, 255, 255, 0.6); }
.emptyTitle { font-weight: 900; margin-bottom: 6px; }
.emptyMuted, .mutedSmall { color: var(--muted); font-size: 12px; }
</style>
