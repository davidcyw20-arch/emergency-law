<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand">
        <div class="logo">EL</div>
        <div>
          <div class="brandTitle">应急普法 · 在线学习</div>
          <div class="brandSub">课程分类 · 课程内容 · 学习进度</div>
        </div>
      </div>

      <div class="right">
        <el-tag v-if="me?.role" effect="light">{{ me.role }}</el-tag>
        <div class="user">
          <div class="userName">{{ me?.nickname || me?.username || '未登录' }}</div>
          <div class="userSub">进度自动保存</div>
        </div>
        <el-button plain @click="logout">退出</el-button>
      </div>
    </header>

    <div class="main">
      <!-- 左：分类与学习记录 -->
      <aside class="side">
        <div class="sideBlock">
          <div class="sideTitle">课程分类</div>
          <div class="sideList">
            <div class="sideItem" :class="{ active: selectedCategoryId === null }" @click="selectCategory(null)">
              全部课程
            </div>
            <div
                v-for="c in categories"
                :key="c.id"
                class="sideItem"
                :class="{ active: selectedCategoryId === c.id }"
                @click="selectCategory(c.id)"
            >
              {{ c.name }}
            </div>
          </div>
        </div>

        <div class="sideBlock">
          <div class="sideTitle">我的学习</div>
          <div v-if="progressList.length === 0" class="emptyMuted">暂无学习记录</div>

          <div v-for="p in progressList" :key="p.id" class="progressRow">
            <div class="pTop">
              <div class="pTitle">
                课程 {{ p.courseId }}
                <span class="mutedSmall">· 课时 {{ p.lessonId || '-' }}</span>
              </div>
              <div class="pPct">{{ p.progressPercent }}%</div>
            </div>
            <el-progress :percentage="p.progressPercent" :stroke-width="8" />
          </div>
        </div>
      </aside>

      <!-- 右：课程列表 -->
      <section class="content">
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="搜索课程标题..." clearable style="max-width: 420px" />
          <div class="mutedText">共 {{ filteredCourses.length }} 门课程</div>
        </div>

        <div v-if="filteredCourses.length === 0" class="emptyCard">
          <div class="emptyTitle">没有找到课程</div>
          <div class="emptyMuted">换个关键词试试，或切换分类</div>
        </div>

        <div v-else class="grid">
          <div v-for="course in filteredCourses" :key="course.id" class="courseCard" @click="goCourse(course)">
            <div class="cover">
              <div class="badge">{{ course.difficulty || 'EASY' }}</div>
              <div class="coverTitle">{{ course.title }}</div>
              <div class="coverSub">{{ course.summary || '暂无简介' }}</div>
            </div>

            <div class="cardBody">
              <div class="metaRow">
                <el-tag size="small" effect="light">分类 {{ course.categoryId }}</el-tag>
                <el-tag size="small" type="success" effect="light">课程 {{ course.id }}</el-tag>
              </div>

              <div class="progressBox">
                <div class="pbTop">
                  <div class="pbTitle">学习进度</div>
                  <div class="pbPct">{{ courseProgressMap[course.id] || 0 }}%</div>
                </div>
                <el-progress :percentage="courseProgressMap[course.id] || 0" :stroke-width="10" />
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiCategories, apiCourses, apiMyProgressList } from '../api/learn'
import { useRouter } from 'vue-router'

const router = useRouter()
const me = ref(null)

const categories = ref([])
const courses = ref([])
const keyword = ref('')
const selectedCategoryId = ref(null)

const progressList = ref([])
const courseProgressMap = ref({})

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.location.href = '/login'
}

function selectCategory(id) {
  selectedCategoryId.value = id
  loadCourses()
}

function goCourse(course) {
  router.push(`/learn/course/${course.id}`)
}

const filteredCourses = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return courses.value
  return courses.value.filter(c => (c.title || '').toLowerCase().includes(k))
})

async function loadMe() {
  try {
    const u = localStorage.getItem('user')
    me.value = u ? JSON.parse(u) : null
  } catch {
    me.value = null
  }
}

async function loadCategories() {
  const res = await apiCategories()
  if (!res.data?.success) throw new Error(res.data?.message || 'load categories failed')
  categories.value = res.data.data || []
}

async function loadCourses() {
  const res = await apiCourses(selectedCategoryId.value)
  if (!res.data?.success) throw new Error(res.data?.message || 'load courses failed')
  courses.value = res.data.data || []
}

async function loadMyProgressList() {
  const res = await apiMyProgressList()
  if (!res.data?.success) throw new Error(res.data?.message || 'load progress failed')
  progressList.value = res.data.data || []

  const map = {}
  for (const p of progressList.value) {
    map[p.courseId] = p.progressPercent || 0
  }
  courseProgressMap.value = map
}

onMounted(async () => {
  await loadMe()
  try {
    await loadCategories()
    await loadCourses()
    await loadMyProgressList()
  } catch (e) {
    ElMessage.error('加载失败：请确认后端 8080 正在运行，且你已登录')
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
.brand { display: flex; align-items: center; gap: 12px; }
.logo {
  width: 38px; height: 38px; border-radius: 14px; display: grid; place-items: center;
  font-weight: 900; color: #fff; background: linear-gradient(135deg, #3b82f6, #22c55e);
  box-shadow: 0 14px 30px rgba(59, 130, 246, 0.22);
}
.brandTitle { font-weight: 900; letter-spacing: 0.3px; }
.brandSub { font-size: 12px; color: var(--muted); margin-top: 2px; }
.right { display: flex; align-items: center; gap: 12px; }
.user { text-align: right; }
.userName { font-weight: 800; }
.userSub { font-size: 12px; color: var(--muted); }

.main { display: grid; grid-template-columns: 290px 1fr; gap: 16px; padding: 16px; }
.side { position: sticky; top: 88px; height: calc(100vh - 104px); overflow: auto; display: grid; gap: 14px; }
.sideBlock {
  background: var(--card); border: 1px solid var(--border); border-radius: 16px;
  box-shadow: 0 20px 55px rgba(15, 23, 42, 0.06); padding: 14px;
}
.sideTitle { font-weight: 900; margin-bottom: 10px; }
.sideList { display: grid; gap: 8px; }
.sideItem {
  padding: 10px 12px; border-radius: 12px; cursor: pointer;
  border: 1px solid rgba(15, 23, 42, 0.06); background: rgba(15, 23, 42, 0.02); transition: 0.15s;
}
.sideItem:hover { transform: translateY(-1px); }
.sideItem.active {
  background: linear-gradient(135deg, rgba(59,130,246,0.14), rgba(34,197,94,0.14));
  border-color: rgba(59,130,246,0.25);
}

.progressRow {
  padding: 10px 10px; border-radius: 12px; border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.7); margin-bottom: 10px;
}
.pTop { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.pTitle { font-weight: 800; font-size: 12px; }
.pPct { font-weight: 900; color: rgba(15, 23, 42, 0.72); }

.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }

.courseCard {
  background: var(--card); border: 1px solid var(--border); border-radius: 16px;
  box-shadow: 0 18px 55px rgba(15, 23, 42, 0.06); overflow: hidden; cursor: pointer; transition: 0.18s;
}
.courseCard:hover { transform: translateY(-3px); box-shadow: 0 22px 60px rgba(15, 23, 42, 0.08); }

.cover { padding: 14px; height: 122px; background: linear-gradient(135deg, rgba(59,130,246,0.18), rgba(34,197,94,0.18)); }
.badge {
  display: inline-flex; font-size: 11px; font-weight: 900; padding: 4px 10px; border-radius: 999px;
  background: rgba(255, 255, 255, 0.92); border: 1px solid rgba(15, 23, 42, 0.08);
}
.coverTitle { margin-top: 10px; font-weight: 900; line-height: 1.25; }
.coverSub {
  margin-top: 6px; font-size: 12px; color: var(--muted);
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}

.cardBody { padding: 12px 14px 14px; }
.metaRow { display: flex; gap: 8px; flex-wrap: wrap; }
.progressBox { margin-top: 12px; }
.pbTop { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.pbTitle { font-weight: 900; font-size: 12px; }
.pbPct { font-weight: 900; color: rgba(15, 23, 42, 0.72); }

.emptyCard { padding: 18px; border-radius: 16px; border: 1px dashed rgba(15, 23, 42, 0.14); background: rgba(255, 255, 255, 0.6); }
.emptyTitle { font-weight: 900; margin-bottom: 6px; }
.emptyMuted, .mutedText, .mutedSmall { color: var(--muted); font-size: 12px; }
</style>
