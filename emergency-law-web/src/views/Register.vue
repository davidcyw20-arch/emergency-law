<template>
  <div class="page">
    <div class="bgGlow g1"></div>
    <div class="bgGlow g2"></div>

    <div class="wrap">
      <div class="card">
        <div class="head">
          <div class="logo">EL</div>
          <div>
            <div class="title">创建账号</div>
            <div class="sub">注册后自动登录，直接进入在线学习</div>
          </div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form">
          <div class="grid">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="建议：字母+数字，例如 test2" clearable />
            </el-form-item>

            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="用于展示" clearable />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入密码" />
            </el-form-item>

            <el-form-item label="地区" prop="region" class="full">
              <el-select
                  v-model="form.region"
                  placeholder="请选择地区"
                  style="width: 100%"
                  filterable
              >
                <el-option v-for="r in REGIONS" :key="r" :label="r" :value="r" />
              </el-select>
            </el-form-item>
          </div>

          <div class="row">
            <el-button @click="goLogin" plain>返回登录</el-button>
            <el-button type="primary" :loading="loading" @click="submit" class="btn">注册并登录</el-button>
          </div>

          <div class="mutedSmall">
            后端默认：<b>http://localhost:8080</b> · 注册接口：<b>POST /api/auth/register</b>
          </div>
        </el-form>
      </div>

      <div class="tips">
        <div class="tipCard">
          <div class="tipTitle">注册说明</div>
          <div class="tipText">我们会把 token 存在浏览器 localStorage，用于访问学习/分享等模块。</div>
        </div>
        <div class="tipCard">
          <div class="tipTitle">演示建议</div>
          <div class="tipText">答辩时现场注册一个账号，再进入学习页展示进度保存，会很加分。</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useRouter } from 'vue-router'
import { REGIONS } from '../constants/regions'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  region: '江苏'
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, message: '用户名至少 3 位', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_, v, cb) => {
        if (v !== form.password) cb(new Error('两次密码不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ],
  region: [{ required: true, message: '请选择地区', trigger: 'change' }]
}

function goLogin() {
  router.push('/login')
}

async function submit() {
  await formRef.value?.validate()

  loading.value = true
  try {
    const rr = await http.post('/api/auth/register', {
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      region: form.region
    })

    if (!rr.data?.success) {
      ElMessage.error(rr.data?.message || '注册失败')
      return
    }

    const lr = await http.post('/api/auth/login', {
      username: form.username,
      password: form.password
    })

    if (!lr.data?.success) {
      ElMessage.success('注册成功，请手动登录')
      router.push('/login')
      return
    }

    const data = lr.data.data
    const token = data?.token
    if (!token) {
      ElMessage.success('注册成功，请手动登录')
      router.push('/login')
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

    localStorage.setItem('remember_username', form.username)

    ElMessage.success('注册并登录成功')
    router.push('/learn')
  } catch (e) {
    ElMessage.error('注册失败：请确认后端 8080 正在运行')
  } finally {
    loading.value = false
  }
}
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

.wrap {
  width: min(980px, 92vw);
  display: grid;
  grid-template-columns: 1fr 0.72fr;
  gap: 16px;
  z-index: 1;
}

.card {
  border-radius: 22px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(15, 23, 42, 0.10);
  backdrop-filter: blur(12px);
  box-shadow: 0 26px 70px rgba(15, 23, 42, 0.10);
}

.head { display: flex; gap: 12px; align-items: center; }
.logo {
  width: 44px; height: 44px; border-radius: 16px;
  display: grid; place-items: center;
  font-weight: 900; color: #fff;
  background: linear-gradient(135deg, #3b82f6, #22c55e);
  box-shadow: 0 16px 40px rgba(59, 130, 246, 0.22);
}
.title { font-weight: 900; font-size: 22px; }
.sub { margin-top: 4px; color: rgba(15, 23, 42, 0.58); font-size: 12px; }

.form { margin-top: 14px; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.full { grid-column: 1 / -1; }

.row { margin-top: 6px; display: flex; justify-content: space-between; gap: 10px; }
.btn { font-weight: 900; border-radius: 14px; }

.tips { display: grid; gap: 12px; }
.tipCard {
  border-radius: 18px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(15, 23, 42, 0.10);
  backdrop-filter: blur(10px);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.08);
}
.tipTitle { font-weight: 900; margin-bottom: 6px; }
.tipText { color: rgba(15, 23, 42, 0.62); line-height: 1.7; font-size: 13px; }

.mutedSmall { margin-top: 10px; color: rgba(15, 23, 42, 0.55); font-size: 12px; }
</style>
