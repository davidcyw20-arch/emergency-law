<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand" @click="goLearn">
        <div class="logo">EL</div>
        <div>
          <div class="brandTitle">经验分享</div>
          <div class="brandSub">交流 · 互动 · 案例沉淀</div>
        </div>
      </div>

      <div class="right">
        <el-button text @click="goLearn" class="navBtn">学习</el-button>
        <el-button text @click="goTest" class="navBtn">在线测试</el-button>
        <el-button text @click="goHistory" class="navBtn">我的成绩</el-button>
        <el-button text @click="goStats" class="navBtn">学习统计</el-button>
        <el-button text @click="goShare" class="navBtn active">经验分享</el-button>

        <el-tag v-if="me?.role" effect="light">{{ me.role }}</el-tag>

        <div class="user">
          <div class="userName">{{ me?.nickname || me?.username || '未登录' }}</div>
          <div class="userSub">共 {{ filtered.length }} 篇</div>
        </div>

        <el-button type="primary" class="pill" @click="openCreate">发布经验</el-button>
        <el-button plain @click="logout">退出</el-button>
      </div>
    </header>

    <div class="main">
      <aside class="side">
        <div class="sideBlock">
          <div class="sideTitle">筛选</div>

          <el-input v-model="keyword" placeholder="搜索标题/内容..." clearable />
          <div style="height: 10px"></div>

          <div class="label">地区</div>
          <el-select v-model="region" placeholder="全部地区" clearable style="width: 100%" filterable>
            <el-option v-for="r in REGIONS" :key="r" :label="r" :value="r" />
          </el-select>

          <div style="height: 10px"></div>

          <div class="label">标签</div>
          <div class="tags">
            <div class="tagChip" :class="{ active: tag === '' }" @click="tag = ''">全部</div>
            <div
                v-for="t in allTags"
                :key="t"
                class="tagChip"
                :class="{ active: tag === t }"
                @click="tag = t"
            >
              {{ t }}
            </div>
          </div>

          <div style="height: 10px"></div>

          <div class="label">排序</div>
          <el-radio-group v-model="sort" class="sortRow">
            <el-radio-button label="latest">最新</el-radio-button>
            <el-radio-button label="hot">最热</el-radio-button>
          </el-radio-group>
        </div>

        <div class="sideBlock">
          <div class="sideTitle">说明</div>
          <div class="mutedSmall">
            该模块用于用户分享应急普法经验与心得，形成案例沉淀与交流互动。<br /><br />
            若后端接口未实现，前端会自动使用本地演示数据（localStorage），不影响答辩展示 UI。
          </div>
        </div>
      </aside>

      <section class="content">
        <div v-if="filtered.length === 0" class="emptyCard">
          <div class="emptyTitle">没有匹配的分享</div>
          <div class="emptyMuted">换个关键词/地区/标签试试</div>
        </div>

        <div v-else class="grid">
          <div v-for="p in filtered" :key="p.id" class="postCard">
            <div class="postHead">
              <div class="postTitle" @click="openDetail(p)">{{ p.title }}</div>
              <el-tag size="small" effect="light">{{ p.region || '未知地区' }}</el-tag>
            </div>

            <div class="postMeta">
              <span class="metaItem">作者：{{ p.author || '匿名' }}</span>
              <span class="dot">·</span>
              <span class="metaItem">时间：{{ fmtTime(p.createdAt) }}</span>
            </div>

            <div class="postContent">
              {{ p.content }}
            </div>

            <div class="postTags">
              <el-tag v-for="t in (p.tags || [])" :key="t" size="small" effect="light" class="tagEl">
                {{ t }}
              </el-tag>
            </div>

            <div class="postActions">
              <el-button size="small" @click="openDetail(p)">📖 查看详情</el-button>
              <el-button size="small" :disabled="p.likedByMe" @click="like(p)">👍 点赞 {{ p.likes || 0 }}</el-button>
              <el-button size="small" @click="toggleComments(p)">💬 评论 {{ p.commentCount || 0 }}</el-button>
              <el-button size="small" type="primary" plain @click="quickUse(p)">
                一键复制为“经验模板”
              </el-button>
            </div>

            <div v-if="commentsMap[p.id]" class="commentPanel">
              <div v-for="c in commentsMap[p.id]" :key="c.id" class="commentItem">
                <span class="commentAuthor">{{ c.author || '匿名' }}：</span>{{ c.content }}
              </div>
              <div class="commentInputRow">
                <el-input v-model="commentInputMap[p.id]" size="small" placeholder="写评论（仅其他用户可评论）" />
                <el-button size="small" type="primary" @click="submitComment(p)">发送</el-button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>



    <el-dialog v-model="detailOpen" title="经验详情" width="760px" class="detailDialog">
      <div v-if="detailPost" class="detailWrap">
        <div class="detailTitle">{{ detailPost.title }}</div>
        <div class="detailMeta">
          <span>作者：{{ detailPost.author || '匿名' }}</span>
          <span>地区：{{ detailPost.region || '未知地区' }}</span>
          <span>发布时间：{{ fmtTime(detailPost.createdAt) }}</span>
        </div>
        <div class="detailTags">
          <el-tag v-for="t in (detailPost.tags || [])" :key="t" size="small" effect="light" class="tagEl">{{ t }}</el-tag>
        </div>
        <div class="detailContent">{{ detailPost.content }}</div>
      </div>
      <template #footer>
        <el-button @click="detailOpen = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createOpen" title="发布经验" width="720px">
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" placeholder="例如：火灾逃生要点总结" />
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input
              v-model="createForm.content"
              type="textarea"
              :rows="6"
              placeholder="写清：场景、做法、注意事项、教训/心得（越具体越好）"
          />
        </el-form-item>

        <div class="dialogGrid">
          <el-form-item label="地区" prop="region">
            <el-select v-model="createForm.region" placeholder="请选择" style="width: 100%" filterable>
              <el-option v-for="r in REGIONS" :key="r" :label="r" :value="r" />
            </el-select>
          </el-form-item>

          <el-form-item label="标签（逗号分隔）" prop="tags">
            <el-input v-model="createForm.tags" placeholder="例如：火灾,逃生,疏散" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="createOpen = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiShareComment, apiShareComments, apiShareCreate, apiShareLike, apiShareList, demoLoad, demoSave } from '../api/share'
import { REGIONS } from '../constants/regions'

const router = useRouter()

const me = ref(null)
const posts = ref([])
const usingDemo = ref(false)

const keyword = ref('')
const region = ref('')
const tag = ref('')
const sort = ref('latest')

const createOpen = ref(false)
const creating = ref(false)
const createRef = ref()
const commentsMap = reactive({})
const commentInputMap = reactive({})
const detailOpen = ref(false)
const detailPost = ref(null)

const createForm = reactive({
  title: '',
  content: '',
  region: '江苏',
  tags: ''
})

const createRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  region: [{ required: true, message: '请选择地区', trigger: 'change' }]
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.location.href = '/login'
}

function goLearn() { router.push('/learn') }
function goStats() { router.push('/stats') }
function goShare() { router.push('/share') }
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

function fmtTime(t) {
  if (!t) return '—'
  try {
    const d = new Date(t)
    const yyyy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    const hh = String(d.getHours()).padStart(2, '0')
    const mi = String(d.getMinutes()).padStart(2, '0')
    return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
  } catch {
    return String(t)
  }
}

async function loadPosts() {
  try {
    const res = await apiShareList()
    if (res.data?.success) {
      posts.value = res.data.data || []
      usingDemo.value = false
      return
    }
    throw new Error(res.data?.message || 'api not ready')
  } catch {
    posts.value = demoLoad()
    usingDemo.value = true
  }
}

const allTags = computed(() => {
  const s = new Set()
  for (const p of posts.value) {
    for (const t of p.tags || []) s.add(t)
  }
  return Array.from(s)
})

const filtered = computed(() => {
  let arr = [...posts.value]
  const k = keyword.value.trim().toLowerCase()
  if (k) {
    arr = arr.filter(
        p =>
            (p.title || '').toLowerCase().includes(k) ||
            (p.content || '').toLowerCase().includes(k)
    )
  }
  if (region.value) arr = arr.filter(p => (p.region || '') === region.value)
  if (tag.value) arr = arr.filter(p => (p.tags || []).includes(tag.value))

  if (sort.value === 'hot') arr.sort((a, b) => (b.likes || 0) - (a.likes || 0))
  else arr.sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')))
  return arr
})

function openDetail(p) {
  detailPost.value = p
  detailOpen.value = true
}

function openCreate() {
  createForm.title = ''
  createForm.content = ''
  createForm.region = '江苏'
  createForm.tags = ''
  createOpen.value = true
}

async function like(p) {
  if (usingDemo.value) {
    p.likes = (p.likes || 0) + 1
    demoSave(posts.value)
    return
  }

  const before = p.likes || 0
  p.likes = before + 1
  try {
    const res = await apiShareLike(p.id)
    if (!res.data?.success) {
      if ((res.data?.message || '').includes('不可重复点赞')) p.likedByMe = true
      throw new Error(res.data?.message || 'like failed')
    }
    if (typeof res.data.data === 'number') p.likes = res.data.data
    p.likedByMe = true
  } catch (e) {
    p.likes = before
    ElMessage.error(e?.response?.data?.message || e?.message || '点赞失败')
  }
}

async function toggleComments(p) {
  if (commentsMap[p.id]) {
    commentsMap[p.id] = null
    return
  }
  try {
    const res = await apiShareComments(p.id)
    commentsMap[p.id] = res.data?.success ? (res.data.data || []) : []
  } catch {
    commentsMap[p.id] = []
  }
}

async function submitComment(p) {
  const content = (commentInputMap[p.id] || '').trim()
  if (!content) return ElMessage.warning('请输入评论内容')

  try {
    const res = await apiShareComment(p.id, { content })
    if (!res.data?.success) throw new Error(res.data?.message || '评论失败')
    commentsMap[p.id] = [...(commentsMap[p.id] || []), res.data.data]
    commentInputMap[p.id] = ''
    p.commentCount = (p.commentCount || 0) + 1
    ElMessage.success('评论成功')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '评论失败')
  }
}

async function submitCreate() {
  await createRef.value?.validate()
  creating.value = true

  const payload = {
    title: createForm.title,
    content: createForm.content,
    region: createForm.region,
    tags: createForm.tags.split(',').map(x => x.trim()).filter(Boolean),
    author: me.value?.nickname || me.value?.username || '匿名'
  }

  try {
    const res = await apiShareCreate(payload)
    if (res.data?.success) {
      ElMessage.success('发布成功')
      createOpen.value = false
      await loadPosts()
      return
    }
    throw new Error(res.data?.message || 'create failed')
  } catch {
    const list = demoLoad()
    const nextId = (list.reduce((m, x) => Math.max(m, Number(x.id || 0)), 0) || 0) + 1
    const item = { id: nextId, ...payload, createdAt: new Date().toISOString(), likes: 0 }
    list.unshift(item)
    demoSave(list)
    posts.value = list
    ElMessage.success('发布成功（本地演示数据）')
    createOpen.value = false
  } finally {
    creating.value = false
  }
}

async function quickUse(p) {
  const template = `【经验标题】${p.title}
【适用场景】（自行补充）
【操作要点】${p.content}
【注意事项】（自行补充）
【来源/地区】${p.region || '—'}
【标签】${(p.tags || []).join(',')}`

  try {
    await navigator.clipboard.writeText(template)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.info('复制失败：浏览器权限限制，可手动复制内容')
  }
}

onMounted(async () => {
  loadMe()
  await loadPosts()
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
.pill { border-radius: 999px; font-weight: 900; }

.user { text-align: right; }
.userName { font-weight: 800; }
.userSub { font-size: 12px; color: var(--muted); }

.main { display: grid; grid-template-columns: 320px 1fr; gap: 16px; padding: 16px; }
.side { position: sticky; top: 88px; height: calc(100vh - 104px); overflow: auto; display: grid; gap: 14px; }
.sideBlock {
  background: var(--card); border: 1px solid var(--border); border-radius: 16px;
  box-shadow: 0 20px 55px rgba(15, 23, 42, 0.06); padding: 14px;
}
.sideTitle { font-weight: 900; margin-bottom: 10px; }
.label { margin: 8px 0 6px; font-weight: 900; font-size: 12px; color: rgba(15,23,42,0.75); }

.tags { display: flex; flex-wrap: wrap; gap: 8px; }
.tagChip {
  padding: 6px 10px; border-radius: 999px;
  border: 1px solid rgba(15,23,42,0.08);
  background: rgba(15,23,42,0.03);
  cursor: pointer;
  font-size: 12px; font-weight: 800;
}
.tagChip.active {
  background: linear-gradient(135deg, rgba(59,130,246,0.14), rgba(34,197,94,0.14));
  border-color: rgba(59,130,246,0.25);
}
.sortRow { display: flex; flex-wrap: wrap; gap: 8px; }

.grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.postCard {
  background: linear-gradient(180deg, rgba(255,255,255,0.98), rgba(247,250,255,0.96));
  border: 1px solid rgba(59,130,246,0.12); border-radius: 18px;
  box-shadow: 0 18px 40px rgba(30, 64, 175, 0.08);
  padding: 14px; display: grid; gap: 10px; transition: transform .2s ease, box-shadow .2s ease;
}
.postCard:hover { transform: translateY(-2px); box-shadow: 0 22px 48px rgba(30,64,175,0.14); }
.postHead { display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; }
.postTitle { font-weight: 900; line-height: 1.25; cursor: pointer; transition: color .2s; }
.postTitle:hover { color: #2563eb; }

.postMeta { color: rgba(15,23,42,0.55); font-size: 12px; }
.dot { margin: 0 6px; color: rgba(15,23,42,0.35); }

.postContent {
  color: rgba(15,23,42,0.78);
  line-height: 1.8;
  white-space: pre-wrap;
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.postTags { display: flex; flex-wrap: wrap; gap: 8px; }
.tagEl { border-radius: 999px; }
.postActions { display: flex; gap: 10px; flex-wrap: wrap; }
.commentPanel { border-top: 1px dashed rgba(15,23,42,0.1); padding-top: 8px; display: grid; gap: 8px; }
.commentItem { font-size: 13px; color: rgba(15,23,42,0.78); }
.commentAuthor { font-weight: 700; }
.commentInputRow { display: grid; grid-template-columns: 1fr auto; gap: 8px; }
.dialogGrid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.emptyCard { padding: 18px; border-radius: 16px; border: 1px dashed rgba(15, 23, 42, 0.14); background: rgba(255, 255, 255, 0.6); }
.emptyTitle { font-weight: 900; margin-bottom: 6px; }
.emptyMuted { color: var(--muted); font-size: 12px; }
.mutedSmall { color: var(--muted); font-size: 12px; line-height: 1.8; }

.detailWrap { display: grid; gap: 12px; }
.detailTitle { font-size: 22px; font-weight: 900; line-height: 1.35; }
.detailMeta { display: flex; flex-wrap: wrap; gap: 12px; color: rgba(15,23,42,0.6); font-size: 12px; }
.detailTags { display: flex; flex-wrap: wrap; gap: 8px; }
.detailContent { white-space: pre-wrap; line-height: 1.9; color: rgba(15,23,42,0.82); background: rgba(59,130,246,0.05); border: 1px solid rgba(59,130,246,0.12); border-radius: 12px; padding: 12px; }
</style>
