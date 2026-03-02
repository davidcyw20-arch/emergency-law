<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand" @click="goTest">
        <div class="logo">EL</div>
        <div>
          <div class="brandTitle">我的成绩</div>
          <div class="brandSub">历史记录 · 统计分析</div>
        </div>
      </div>

      <div class="right">
        <el-button text @click="goLearn" class="navBtn">学习</el-button>
        <el-button text @click="goTest" class="navBtn">在线测试</el-button>
        <el-button text @click="goHistory" class="navBtn active">我的成绩</el-button>
        <el-button text @click="goStats" class="navBtn">学习统计</el-button>
        <el-button text @click="goShare" class="navBtn">经验分享</el-button>

        <div class="user">
          <div class="userName">{{ me?.nickname || me?.username || '未登录' }}</div>
          <div class="userSub">{{ rows.length }} 条记录</div>
        </div>
        <el-button plain @click="logout">退出</el-button>
      </div>
    </header>

    <div class="wrap">
      <div class="cards">
        <div class="kpiCard">
          <div class="kpiTitle">平均分</div>
          <div class="kpiValue">{{ avgScore }}</div>
          <div class="kpiSub">近 {{ rows.length }} 次测试</div>
        </div>
        <div class="kpiCard">
          <div class="kpiTitle">最高分</div>
          <div class="kpiValue">{{ maxScore }}</div>
          <div class="kpiSub">最佳表现</div>
        </div>
        <div class="kpiCard">
          <div class="kpiTitle">总用时</div>
          <div class="kpiValue">{{ sumMin }} min</div>
          <div class="kpiSub">累计答题时间</div>
        </div>
        <div class="kpiCard">
          <div class="kpiTitle">最近一次</div>
          <div class="kpiValue small">{{ latestTime }}</div>
          <div class="kpiSub">最近提交</div>
        </div>
      </div>

      <div class="panel">
        <div class="panelTitle">成绩记录</div>
        <el-table :data="rows" stripe style="width: 100%">
          <el-table-column prop="id" label="记录ID" width="90" />
          <el-table-column prop="paperId" label="试卷ID" width="90" />
          <el-table-column prop="score" label="分数" width="90" />
          <el-table-column label="正确/总题" width="120">
            <template #default="{ row }">
              {{ row.correctCount }} / {{ row.total }}
            </template>
          </el-table-column>
          <el-table-column prop="usedMin" label="用时(min)" width="110" />
          <el-table-column prop="createdAt" label="提交时间" min-width="200" />
        </el-table>

        <div v-if="rows.length === 0" class="emptyMuted" style="margin-top: 10px;">
          还没有成绩记录。去 /test 做一套试卷吧。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiTestMyHistory } from '../api/test'

const router = useRouter()
const me = ref(null)
const rows = ref([])

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

const avgScore = computed(() => {
  if (rows.value.length === 0) return 0
  const s = rows.value.reduce((a, b) => a + (b.score || 0), 0)
  return Math.round(s / rows.value.length)
})
const maxScore = computed(() => {
  if (rows.value.length === 0) return 0
  return Math.max(...rows.value.map(x => x.score || 0))
})
const sumMin = computed(() => rows.value.reduce((a, b) => a + (b.usedMin || 0), 0))
const latestTime = computed(() => rows.value[0]?.createdAt || '—')

onMounted(async () => {
  try {
    me.value = JSON.parse(localStorage.getItem('user') || 'null')
  } catch { me.value = null }

  try {
    const res = await apiTestMyHistory()
    if (!res.data?.success) throw new Error(res.data?.message || 'load failed')
    rows.value = res.data.data || []
  } catch (e) {
    ElMessage.error('加载成绩失败：请确认后端 8080 正常运行')
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
.cards { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.kpiCard {
  background: var(--card); border: 1px solid var(--border); border-radius: 16px;
  box-shadow: 0 20px 55px rgba(15, 23, 42, 0.06); padding: 14px;
}
.kpiTitle { font-weight: 900; color: rgba(15,23,42,0.7); font-size: 12px; }
.kpiValue { margin-top: 10px; font-weight: 900; font-size: 28px; }
.kpiValue.small { font-size: 16px; }
.kpiSub { margin-top: 6px; color: var(--muted); font-size: 12px; }

.panel {
  background: var(--card); border: 1px solid var(--border); border-radius: 16px;
  box-shadow: 0 20px 55px rgba(15, 23, 42, 0.06); padding: 14px;
}
.panelTitle { font-weight: 900; margin-bottom: 10px; }
.emptyMuted { color: var(--muted); font-size: 12px; }
</style>
