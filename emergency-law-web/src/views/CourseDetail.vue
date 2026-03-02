<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand" @click="goBack">
        <div class="backBtn">←</div>
        <div>
          <div class="brandTitle">课程详情</div>
          <div class="brandSub">/learn/course/{{ courseId }}</div>
        </div>
      </div>

      <div class="right">
        <el-button text @click="goLearn" class="navBtn">学习</el-button>
        <el-button text @click="goTest" class="navBtn">在线测试</el-button>
        <el-button text @click="goHistory" class="navBtn">我的成绩</el-button>
        <el-button text @click="goStats" class="navBtn">学习统计</el-button>
        <el-button text @click="goShare" class="navBtn">经验分享</el-button>

        <el-tag v-if="me?.role" effect="light">{{ me.role }}</el-tag>
        <div class="user">
          <div class="userName">{{ me?.nickname || me?.username || '未登录' }}</div>
          <div class="userSub">进度自动保存</div>
        </div>
        <el-button plain @click="logout">退出</el-button>
      </div>
    </header>

    <div class="wrap">
      <aside class="left">
        <div class="panel">
          <div class="courseCover" :style="courseCoverStyle"></div>
          <div class="title">{{ course?.title || '加载中...' }}</div>
          <div class="sub">{{ course?.summary || '暂无简介' }}</div>

          <div class="progressBox">
            <div class="pbTop">
              <div class="pbTitle">课程进度</div>
              <div class="pbPct">{{ progress }}%</div>
            </div>
            <el-slider v-model="progress" :step="5" show-stops @change="syncProgress" />
            <div class="mutedSmall">{{ saving ? '保存中...' : '拖动后自动保存' }}</div>
          </div>

          <div class="metaLine">
            <el-tag size="small" effect="light">{{ course?.difficulty || 'EASY' }}</el-tag>
            <el-tag v-if="course?.enabled === 1" size="small" type="success" effect="light">可学习</el-tag>
          </div>
        </div>

        <div class="panel">
          <div class="panelTitle">课时目录</div>
          <div v-if="lessons.length === 0" class="emptyMuted">暂无课时</div>

          <div
              v-for="l in lessons"
              :key="l.id"
              class="lessonItem"
              :class="{ active: currentLesson?.id === l.id }"
              @click="openLesson(l)"
          >
            <div class="lessonName">{{ l.title }}</div>
            <div class="lessonMeta">
              <span class="mutedSmall">{{ l.durationMin }} min</span>
              <span class="dot">·</span>
              <span class="mutedSmall">{{ l.contentType }}</span>
            </div>
          </div>
        </div>
      </aside>

      <section class="rightPanel">
        <div class="panel">
          <div class="contentTop">
            <div>
              <div class="contentTitle">{{ currentLesson?.title || '请选择课时开始学习' }}</div>
              <div class="contentSub" v-if="lessons.length > 0">当前课时：{{ lessonIndexText }}</div>
            </div>

            <div class="navLessonBtns" v-if="lessons.length > 0">
              <el-button size="small" :disabled="!hasPrev" @click="goPrev">上一课时</el-button>
              <el-button size="small" :disabled="!hasNext" @click="goNext">下一课时</el-button>
            </div>
          </div>

          <div v-if="!currentLesson" class="emptyCard">
            <div class="emptyTitle">从左侧选择一个课时</div>
            <div class="emptyMuted">你的学习进度会同步到后端</div>
          </div>

          <div v-else class="contentBox">
            <template v-if="lessonDetail?.contentType === 'TEXT'">
              <div class="textContent">
                {{ lessonDetail?.contentText || '暂无内容' }}
              </div>
            </template>

            <template v-else-if="lessonDetail?.contentType === 'LINK'">
              <el-link :href="lessonDetail?.contentUrl" target="_blank" type="primary">
                打开学习链接
              </el-link>
            </template>

            <template v-else-if="lessonDetail?.contentType === 'VIDEO'">
              <template v-if="isVideoFileUrl(lessonDetail?.contentUrl)">
                <video class="videoPlayer" :src="lessonDetail?.contentUrl" controls preload="metadata" />
              </template>
              <template v-else>
                <div class="emptyMuted">当前为视频页面链接，可点击跳转观看</div>
                <el-link :href="lessonDetail?.contentUrl" target="_blank" type="primary">
                  打开视频链接
                </el-link>
              </template>
            </template>

            <div class="actionsRow">
              <el-button @click="saveLearning">保存为学习中</el-button>
              <el-button type="primary" @click="saveDone">本课时学完</el-button>
            </div>

            <div class="mutedSmall">
              小技巧：完成后点击“下一课时”继续学习
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiCourseLessons, apiLesson, apiMyProgress, apiUpsertProgress, apiGetCourse } from '../api/learn'

const route = useRoute()
const router = useRouter()

const me = ref(null)

const courseId = String(route.params.id)
const course = ref(null)

function idEq(a, b) {
  return String(a) === String(b)
}

function resolveCourseCover(url) {
  const u = String(url || '').trim()
  if (!u) return '/covers/course-default.svg'
  if (/^[a-zA-Z]:\\/.test(u) || u.includes(':\\') || u.startsWith('file:')) return '/covers/course-default.svg'
  if (u.startsWith('http://') || u.startsWith('https://') || u.startsWith('data:') || u.startsWith('/')) return u
  return `/${u.replace(/^\.?\//, '')}`
}

const courseCoverStyle = computed(() => {
  const u = resolveCourseCover(course.value?.coverUrl)
  return {
    backgroundImage:
      `linear-gradient(135deg, rgba(59,130,246,0.20), rgba(34,197,94,0.18)), url('${u}')`
  }
})

const lessons = ref([])
const currentLesson = ref(null)
const lessonDetail = ref(null)
const progress = ref(0)
const saving = ref(false)

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
function goBack() { router.push('/learn') }

function isVideoFileUrl(url) {
  const u = String(url || '').toLowerCase().split('?')[0]
  return u.startsWith('data:video/') || u.endsWith('.mp4') || u.endsWith('.webm') || u.endsWith('.ogg') || u.endsWith('.m3u8')
}

async function loadMe() {

  try {
    const u = localStorage.getItem('user')
    me.value = u ? JSON.parse(u) : null
  } catch {
    me.value = null
  }
}

async function loadCourse() {
  const res = await apiGetCourse(courseId)
  if (!res.data?.success) throw new Error(res.data?.message || 'load course failed')
  course.value = res.data.data
}

async function loadLessons() {
  const lr = await apiCourseLessons(courseId)
  if (!lr.data?.success) throw new Error(lr.data?.message || 'load lessons failed')
  lessons.value = lr.data.data || []
}

async function loadProgressAndAutoOpen() {
  const pr = await apiMyProgress(courseId)
  if (pr.data?.success && pr.data.data) {
    progress.value = pr.data.data.progressPercent || 0
    const lastLessonId = pr.data.data.lessonId
    if (lastLessonId) {
      const hit = lessons.value.find(x => idEq(x.id, lastLessonId))
      if (hit) {
        await openLesson(hit)
        return
      }
    }
  }

  if (lessons.value.length > 0) {
    await openLesson(lessons.value[0])
  } else {
    progress.value = 0
  }
}

async function openLesson(lesson, options = {}) {
  currentLesson.value = lesson
  const res = await apiLesson(lesson.id)
  if (!res.data?.success) throw new Error(res.data?.message || 'load lesson failed')
  lessonDetail.value = res.data.data

  if (!options.skipProgressSync) {
    await apiUpsertProgress({
      courseId,
      lessonId: lesson.id,
      progressPercent: Math.max(5, progress.value || 0),
      status: 'LEARNING'
    })
  }
}

async function syncProgress() {
  saving.value = true
  try {
    const pct = Math.min(100, Math.max(0, Math.round((progress.value || 0) / 5) * 5))
    progress.value = pct
    await apiUpsertProgress({
      courseId,
      lessonId: currentLesson.value?.id ?? null,
      progressPercent: pct,
      status: pct >= 100 ? 'DONE' : 'LEARNING'
    })
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存进度失败')
  } finally {
    saving.value = false
  }
}

async function saveLearning() {
  saving.value = true
  try {
    const pct = Math.max(5, Math.min(100, Math.round((progress.value || 0) / 5) * 5))
    progress.value = pct
    await apiUpsertProgress({
      courseId,
      lessonId: currentLesson.value?.id ?? null,
      progressPercent: pct,
      status: 'LEARNING'
    })
    ElMessage.success('已保存为学习中')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function saveDone() {
  saving.value = true
  try {
    const idx = currentIndex.value
    const total = lessons.value.length || 0
    const base = (idx >= 0 && total > 0) ? Math.round(((idx + 1) * 100) / total) : 100
    const nextPct = Math.min(100, Math.max(progress.value || 0, base))
    const pct = Math.min(100, Math.max(0, Math.round(nextPct / 5) * 5))
    progress.value = pct

    await apiUpsertProgress({
      courseId,
      lessonId: currentLesson.value?.id ?? null,
      progressPercent: pct,
      status: pct >= 100 ? 'DONE' : 'LEARNING'
    })

    if (pct >= 100) {
      ElMessage.success('课程已完成')
      return
    }

    if (hasNext.value) {
      ElMessage.success('已完成本课时，已进入下一课时')
      await openLesson(lessons.value[currentIndex.value + 1], { skipProgressSync: true })
    } else {
      ElMessage.success('已完成')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const currentIndex = computed(() => {
  if (!currentLesson.value) return -1
  return lessons.value.findIndex(x => idEq(x.id, currentLesson.value.id))
})

const lessonIndexText = computed(() => {
  if (currentIndex.value < 0) return `- / ${lessons.value.length}`
  return `${currentIndex.value + 1} / ${lessons.value.length}`
})

const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value >= 0 && currentIndex.value < lessons.value.length - 1)

async function goPrev() {
  if (!hasPrev.value) return
  await openLesson(lessons.value[currentIndex.value - 1])
}
async function goNext() {
  if (!hasNext.value) return
  await openLesson(lessons.value[currentIndex.value + 1])
}

onMounted(async () => {
  await loadMe()
  try {
    await loadCourse()
    await loadLessons()
    await loadProgressAndAutoOpen()
  } catch (e) {
    ElMessage.error('加载失败：请确认后端 8080 正在运行')
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
.backBtn {
  width: 36px; height: 36px; border-radius: 12px; display: grid; place-items: center;
  background: rgba(15, 23, 42, 0.04); border: 1px solid rgba(15, 23, 42, 0.08);
  font-weight: 900;
}
.brandTitle { font-weight: 900; }
.brandSub { font-size: 12px; color: var(--muted); margin-top: 2px; }

.right { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.navBtn { font-weight: 800; }
.navBtn.active { color: #2563eb; }

.user { text-align: right; }
.userName { font-weight: 800; }
.userSub { font-size: 12px; color: var(--muted); }

.wrap { display: grid; grid-template-columns: 360px 1fr; gap: 16px; padding: 16px; }
.left { display: grid; gap: 14px; position: sticky; top: 88px; height: calc(100vh - 104px); overflow: auto; }
.rightPanel { min-height: 400px; }

.panel {
  background: var(--card); border: 1px solid var(--border); border-radius: 16px;
  box-shadow: 0 20px 55px rgba(15, 23, 42, 0.06); padding: 14px;
}
.courseCover {
  height: 140px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.10);
  background-size: cover;
  background-position: center;
  margin-bottom: 12px;
}
.title { font-weight: 900; font-size: 16px; }
.sub { margin-top: 6px; font-size: 12px; color: var(--muted); }
.metaLine { margin-top: 10px; display: flex; gap: 8px; flex-wrap: wrap; }

.panelTitle { font-weight: 900; margin-bottom: 10px; }

.lessonItem {
  padding: 10px 10px; border-radius: 12px; border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(15, 23, 42, 0.02); cursor: pointer; margin-bottom: 10px;
}
.lessonItem.active {
  background: linear-gradient(135deg, rgba(59,130,246,0.14), rgba(34,197,94,0.14));
  border-color: rgba(59,130,246,0.25);
}
.lessonName { font-weight: 900; }
.lessonMeta { margin-top: 4px; font-size: 12px; color: var(--muted); }
.dot { margin: 0 6px; color: rgba(15, 23, 42, 0.35); }

.progressBox { margin-top: 14px; padding-top: 14px; border-top: 1px solid rgba(15, 23, 42, 0.06); }
.pbTop { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.pbTitle { font-weight: 900; font-size: 12px; }
.pbPct { font-weight: 900; color: rgba(15, 23, 42, 0.72); }

.contentTop { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 10px; }
.contentTitle { font-weight: 900; }
.contentSub { margin-top: 4px; color: rgba(15, 23, 42, 0.55); font-size: 12px; }

.navLessonBtns { display: flex; gap: 8px; }
.contentBox { display: grid; gap: 12px; }
.textContent {
  white-space: pre-wrap; line-height: 1.9; color: rgba(15, 23, 42, 0.88);
  padding: 14px; border-radius: 14px; border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.75);
}
.actionsRow { display: flex; gap: 10px; }

.emptyCard { padding: 18px; border-radius: 16px; border: 1px dashed rgba(15, 23, 42, 0.14); background: rgba(255, 255, 255, 0.6); }
.emptyTitle { font-weight: 900; margin-bottom: 6px; }
.emptyMuted, .mutedSmall { color: var(--muted); font-size: 12px; }
.videoPlayer { width: min(860px, 100%); max-height: 460px; border-radius: 14px; border: 1px solid var(--border); background: #000; }
</style>
