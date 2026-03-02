<template>
  <div class="page">
    <div class="bgGlow g1"></div>
    <div class="bgGlow g2"></div>

    <div class="wrap">
      <div class="left">
        <div class="hero">
          <div class="heroLogo">EL</div>
          <div class="heroTitle">应急普法系统</div>
          <div class="heroSub">
            在线学习 · 在线测试 · 经验分享<br />
            让普法更精准、更持续、更有趣
          </div>

          <div class="heroCard">
            <div class="hcTitle">今日小提示</div>
            <div class="hcText">
              突发事件处置要点：先保证自身安全，再协助他人；遇到不确定风险，保持距离并及时报警/上报。
            </div>
          </div>

          <div class="heroFooter">
            <el-tag effect="light" type="primary">前端：Vue 3 + Vite + Element Plus</el-tag>
            <el-tag effect="light" type="success">后端：Spring Boot + MyBatis-Plus</el-tag>
          </div>
        </div>
      </div>

      <div class="right">
        <div class="card">
          <div class="title">登录</div>
          <div class="subtitle">使用你的账号进入在线学习</div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" clearable />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>

            <div class="row">
              <el-checkbox v-model="remember">记住我</el-checkbox>
              <el-link type="primary" @click="fillDemo">填充演示账号</el-link>
            </div>

            <el-button type="primary" class="btn" :loading="loading" @click="submit">
              登录
            </el-button>

            <div class="tips">
              <span class="muted">没有账号？</span>
              <el-link type="primary" @click="goRegister">去注册</el-link>
            </div>

            <div class="mutedSmall">
              后端地址默认：<b>http://localhost:8080</b>（请确保后端已启动）
            </div>
          </el-form>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useRouter } from 'vue-router'

const router = useRouter()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const remember = ref(true)

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function fillDemo() {
  form.username = 'test1'
  form.password = '123456'
  ElMessage.success('已填充演示账号')
}

function goRegister() {
  router.push('/register')
}

async function submit() {
  // validate 失败会抛异常，必须 catch，不然后续逻辑根本不走
  try {
    await formRef.value.validate()
  } catch (e) {
    // 校验不通过直接返回
    return
  }

  loading.value = true
  try {
    // http.js 响应拦截器如果是 resp.data，则这里 res 就是后端的 {success,message,data}
    const res = await http.post('/api/auth/login', {
      username: form.username,
      password: form.password
    })

    // 兼容两种情况：
    // 1) http.js 没拆包：res.data.success
    // 2) http.js 已拆包：res.success
    const ok = res?.success ?? res?.data?.success
    const msg = res?.message ?? res?.data?.message
    const data = res?.data?.data ?? res?.data

    if (!ok) {
      ElMessage.error(msg || '登录失败')
      return
    }

    const token = data?.token
    if (!token) {
      ElMessage.error('登录失败：未返回 token')
      return
    }

    localStorage.setItem('token', token)
    localStorage.setItem(
        'user',
        JSON.stringify({
          userId: data.userId,
          username: data.username,
          nickname: data.nickname,
          role: data.role
        })
    )

    if (remember.value) localStorage.setItem('remember_username', form.username)
    else localStorage.removeItem('remember_username')

    ElMessage.success('登录成功')
    const target = data?.role === 'ADMIN' ? '/admin' : '/learn'
    router.push(target)
  } catch (e) {
    ElMessage.error('登录失败：请确认后端 8080 正在运行')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const u = localStorage.getItem('remember_username')
  if (u) form.username = u
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: radial-gradient(1200px 800px at 20% 10%, #e8f1ff 0%, #f7f9fc 45%, #f6f7fb 100%);
  position: relative;
  overflow: hidden;
}
.bgGlow { position: absolute; filter: blur(60px); opacity: 0.55; border-radius: 999px; }
.g1 { width: 520px; height: 520px; left: -140px; top: -120px; background: #60a5fa; }
.g2 { width: 520px; height: 520px; right: -160px; bottom: -160px; background: #34d399; }

.wrap { width: min(1100px, 92vw); display: grid; grid-template-columns: 1.1fr 0.9fr; gap: 18px; z-index: 1; }
.left, .right { display: flex; align-items: stretch; }

.hero {
  flex: 1; border-radius: 22px; padding: 26px;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(15, 23, 42, 0.10);
  backdrop-filter: blur(12px);
  box-shadow: 0 26px 70px rgba(15, 23, 42, 0.10);
}
.heroLogo {
  width: 56px; height: 56px; border-radius: 18px; display: grid; place-items: center;
  font-weight: 900; color: #fff; font-size: 18px;
  background: linear-gradient(135deg, #3b82f6, #22c55e);
  box-shadow: 0 16px 40px rgba(59, 130, 246, 0.22);
}
.heroTitle { margin-top: 14px; font-weight: 900; font-size: 26px; letter-spacing: 0.5px; }
.heroSub { margin-top: 10px; color: rgba(15, 23, 42, 0.62); line-height: 1.7; }
.heroCard {
  margin-top: 18px; padding: 14px; border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.08);
}
.hcTitle { font-weight: 900; margin-bottom: 6px; }
.hcText { color: rgba(15, 23, 42, 0.68); line-height: 1.7; font-size: 13px; }
.heroFooter { margin-top: 14px; display: flex; gap: 10px; flex-wrap: wrap; }

.card {
  flex: 1; border-radius: 22px; padding: 22px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(15, 23, 42, 0.10);
  backdrop-filter: blur(12px);
  box-shadow: 0 26px 70px rgba(15, 23, 42, 0.10);
}
.title { font-weight: 900; font-size: 22px; }
.subtitle { margin-top: 6px; color: rgba(15, 23, 42, 0.58); }

.form { margin-top: 14px; }
.row { display: flex; justify-content: space-between; align-items: center; margin: 4px 0 10px; }
.btn { width: 100%; height: 42px; border-radius: 14px; font-weight: 900; }

.tips { margin-top: 10px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.muted { color: rgba(15, 23, 42, 0.55); }
.mutedSmall { margin-top: 10px; color: rgba(15, 23, 42, 0.55); font-size: 12px; }

</style>
