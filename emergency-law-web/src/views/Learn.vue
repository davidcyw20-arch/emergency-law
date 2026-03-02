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
        <el-button
            v-if="latestProgress"
            type="primary"
            plain
            @click="continueLatest"
            class="primaryPill"
        >
          继续上次学习：{{ latestTitle }}
        </el-button>

        <el-button text @click="goLearn" class="navBtn" :class="{ active: isLearn }">学习</el-button>
        <el-button text @click="goTest" class="navBtn" :class="{ active: isTest }">在线测试</el-button>
        <el-button text @click="goHistory" class="navBtn" :class="{ active: isHistory }">我的成绩</el-button>
        <el-button text @click="goStats" class="navBtn" :class="{ active: isStats }">学习统计</el-button>
        <el-button text @click="goShare" class="navBtn" :class="{ active: isShare }">经验分享</el-button>

        <el-tag v-if="me?.role" effect="light">{{ me.role }}</el-tag>

        <div class="user">
          <div class="userName">{{ me?.nickname || me?.username || '未登录' }}</div>
          <div class="userSub">进度自动保存</div>
        </div>

        <el-button plain @click="logout">退出</el-button>
      </div>
    </header>

    <div class="main">
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

          <div v-for="p in progressList" :key="p.id" class="progressRow" @click="goCourse(p.courseId)">
            <div class="pTop">
              <div class="pTitle">
                {{ courseTitleMap[p.courseId] || ('课程 ' + p.courseId) }}
                <span class="mutedSmall">· 课时 {{ p.lessonId || '-' }}</span>
              </div>
              <div class="pPct">{{ p.progressPercent }}%</div>
            </div>
            <el-progress :percentage="p.progressPercent" :stroke-width="8" />
            <div class="rowHint">点击继续学习</div>
          </div>
        </div>
      </aside>

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
          <div v-for="course in filteredCourses" :key="course.id" class="courseCard">
            <div class="cover" @click="goCourse(course.id)">
              <img
                class="coverImg"
                :src="resolveCourseCover(course.coverUrl)"
                alt="cover"
                loading="lazy"
                @error="onCoverError"
              />
              <div class="badge">{{ course.difficulty || 'EASY' }}</div>
              <div class="coverTitle">{{ course.title }}</div>
              <div class="coverSub">{{ course.summary || '暂无简介' }}</div>
            </div>

            <div class="cardBody">
              <div class="metaRow">
                <el-tag size="small" effect="light">{{ getCategoryName(course.categoryId) }}</el-tag>
              </div>

              <div class="progressBox">
                <div class="pbTop">
                  <div class="pbTitle">学习进度</div>
                  <div class="pbPct">{{ courseProgressMap[course.id] || 0 }}%</div>
                </div>
                <el-progress :percentage="courseProgressMap[course.id] || 0" :stroke-width="10" />
              </div>

              <div class="actionRow">
                <el-button size="small" @click="goCourse(course.id)">进入课程</el-button>
                <el-button
                    size="small"
                    type="primary"
                    @click="goCourse(course.id)"
                    :disabled="!progressMap[course.id]"
                >
                  继续学习
                </el-button>
              </div>

              <div v-if="progressMap[course.id]" class="hintLine">
                上次学习：{{ progressMap[course.id].progressPercent || 0 }}% · {{ String(progressMap[course.id].lastStudyTime || '').substring(0, 16).replace('T', ' ') || '-' }}
              </div>
              <div v-else class="hintLine mutedSmall">还未学习过</div>
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
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const me = ref(null)

const categories = ref([])
const courses = ref([])
const keyword = ref('')
const selectedCategoryId = ref(null)

const progressList = ref([])
const courseProgressMap = ref({})
const progressMap = ref({})

const isLearn = computed(() => route.path.startsWith('/learn'))
const isStats = computed(() => route.path.startsWith('/stats'))
const isShare = computed(() => route.path.startsWith('/share'))
const isTest = computed(() => route.path === '/test' || route.path.startsWith('/test/take'))
const isHistory = computed(() => route.path.startsWith('/test/history'))

const courseTitleMap = computed(() => {
  const m = {}
  for (const c of courses.value) m[String(c.id)] = c.title
  return m
})

const latestProgress = computed(() => {
  const arr = progressList.value || []
  if (arr.length === 0) return null
  return arr.reduce((a, b) => (String(a.lastStudyTime) >= String(b.lastStudyTime) ? a : b))
})

const latestTitle = computed(() => {
  if (!latestProgress.value) return ''
  const id = String(latestProgress.value.courseId)
  return courseTitleMap.value[id] || `课程 ${id}`
})

function continueLatest() {
  if (!latestProgress.value) return
  goCourse(latestProgress.value.courseId)
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.location.href = '/login'
}

function goStats() { router.push('/stats') }
function goLearn() { router.push('/learn') }
function goShare() { router.push('/share') }
function goTest() { router.push('/test') }
function goHistory() { router.push('/test/history') }

function selectCategory(id) {
  selectedCategoryId.value = id
  loadCourses()
}

function getCategoryName(categoryId) {
  const hit = categories.value.find(c => String(c.id) === String(categoryId))
  return hit ? hit.name : `分类 ${categoryId}`
}

function goCourse(courseId) {
  router.push(`/learn/course/${courseId}`)
}

function resolveCourseCover(url) {
  const u = String(url || '').trim()
  if (!u) return '/covers/course-default.svg'
  if (/^[a-zA-Z]:\\/.test(u) || u.includes(':\\') || u.startsWith('file:')) return '/covers/course-default.svg'
  if (u.startsWith('http://') || u.startsWith('https://') || u.startsWith('data:') || u.startsWith('/')) return u
  return `/${u.replace(/^\.?\//, '')}`
}

function onCoverError(e) {
  const img = e?.target
  if (!img) return
  img.src = '/covers/course-default.svg'
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

  const percentMap = {}
  const objMap = {}
  for (const p of progressList.value) {
    percentMap[p.courseId] = p.progressPercent || 0
    objMap[p.courseId] = p
  }
  courseProgressMap.value = percentMap
  progressMap.value = objMap
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

.right { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.navBtn { font-weight: 800; }
.navBtn.active { color: #2563eb; }

.primaryPill { border-radius: 999px; font-weight: 900; }

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
  background: rgba(255, 255, 255, 0.7); margin-bottom: 10px; cursor: pointer;
}
.pTop { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.pTitle { font-weight: 800; font-size: 12px; }
.pPct { font-weight: 900; color: rgba(15, 23, 42, 0.72); }
.rowHint { margin-top: 6px; font-size: 12px; color: rgba(15, 23, 42, 0.45); }

.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }

.courseCard {
  background: var(--card); border: 1px solid var(--border); border-radius: 16px;
  box-shadow: 0 18px 55px rgba(15, 23, 42, 0.06); overflow: hidden; transition: 0.18s;
}
.courseCard:hover { transform: translateY(-3px); box-shadow: 0 22px 60px rgba(15, 23, 42, 0.08); }

.cover {
  padding: 14px;
  height: 122px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.cover::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(59,130,246,0.20), rgba(34,197,94,0.18));
  z-index: 1;
}
.coverImg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
}
.badge, .coverTitle, .coverSub { position: relative; z-index: 2; }
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
.actionRow { margin-top: 12px; display: flex; justify-content: space-between; gap: 10px; }
.hintLine { margin-top: 8px; font-size: 12px; color: rgba(15, 23, 42, 0.65); }

.emptyCard { padding: 18px; border-radius: 16px; border: 1px dashed rgba(15, 23, 42, 0.14); background: rgba(255, 255, 255, 0.6); }
.emptyTitle { font-weight: 900; margin-bottom: 6px; }
.emptyMuted, .mutedText, .mutedSmall { color: var(--muted); font-size: 12px; }
</style>
