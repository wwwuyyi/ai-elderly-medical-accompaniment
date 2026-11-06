<template>
  <div class="login-container">
    <div class="login-card">
      <!-- Logo Area -->
      <div class="logo-area">
        <div class="logo-icon">
          <svg viewBox="0 0 64 64" width="64" height="64">
            <circle cx="32" cy="32" r="30" fill="#409EFF" />
            <path d="M20 38 Q32 16 44 38" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" />
            <circle cx="32" cy="26" r="6" fill="#fff" />
          </svg>
        </div>
        <h1 class="app-title">AI赋能陪诊助医服务系统</h1>
        <p class="app-subtitle">关爱老人，智慧陪诊</p>
      </div>

      <!-- Role Tabs -->
      <div class="role-tabs">
        <div
          v-for="role in roles"
          :key="role.value"
          :class="['role-tab', { active: loginForm.role === role.value }]"
          @click="loginForm.role = role.value"
        >
          {{ role.label }}
        </div>
      </div>

      <!-- Login Form -->
      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item prop="captcha">
          <div class="captcha-row">
            <el-input v-model="loginForm.captcha" placeholder="4位数字验证码" clearable class="captcha-input" />
            <div class="captcha-img" @click="refreshCaptcha" title="点击刷新">
              <img v-if="captchaSvg" :src="captchaSvg" alt="验证码" />
              <span v-else class="captcha-placeholder">加载中</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>

        <div class="form-footer">
          <span class="register-link" @click="$router.push('/register')">
            还没有账号？<span class="link-text">立即注册</span>
          </span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/api/request'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const captchaSvg = ref('')
const captchaKey = ref('')

const roles = [
  { label: '家属端', value: 'FAMILY' },
  { label: '陪诊师端', value: 'ESCORT' },
  { label: '管理端', value: 'ADMIN' }
]

const loginForm = reactive({
  username: '',
  password: '',
  captcha: '',
  role: 'FAMILY'
})

const rules = {
  username: [
    { required: true, message: '请输入手机号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度在6到30个字符', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

const roleRedirectMap = {
  FAMILY: '/family/home',
  ESCORT: '/escort/home',
  ADMIN: '/admin/dashboard'
}

const fetchCaptcha = async () => {
  try {
    const res = await request.get('/auth/captcha')
    if (res.data) {
      captchaKey.value = res.data.captchaKey || ''
      captchaSvg.value = res.data.captchaImage || ''
    }
  } catch {
    ElMessage.warning('验证码加载失败，请点击刷新')
  }
}

const refreshCaptcha = () => {
  captchaSvg.value = ''
  fetchCaptcha()
}

const handleLogin = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await request.post('/auth/login', {
      username: loginForm.username,
      password: loginForm.password,
      captcha: loginForm.captcha,
      captchaKey: captchaKey.value,
      role: loginForm.role
    })

    const { token, userId, username, nickname, role, avatar } = res.data

    if (token) {
      userStore.setToken(token)
      // 后端返回平铺字段，手动组装 user 对象存入 localStorage
      const user = { id: userId, username, nickname, role, avatar }
      userStore.setUser(user)
      ElMessage.success('登录成功')
      const redirectPath = roleRedirectMap[role] || roleRedirectMap[loginForm.role] || '/family/home'
      router.push(redirectPath)
    } else {
      ElMessage.error('登录失败，未获取到令牌')
    }
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || '登录失败，请重试'
    ElMessage.error(msg)
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCaptcha()
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f4ff 0%, #f0f7ff 50%, #e6f0fa 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 440px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(64, 158, 255, 0.12);
  padding: 48px 40px 40px;
}

.logo-area {
  text-align: center;
  margin-bottom: 32px;
}

.logo-icon {
  margin-bottom: 16px;
}

.app-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 8px 0;
  letter-spacing: 1px;
}

.app-subtitle {
  font-size: 15px;
  color: #909399;
  margin: 0;
}

.role-tabs {
  display: flex;
  background: #f5f7fa;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 28px;
}

.role-tab {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  font-size: 18px;
  font-weight: 500;
  color: #909399;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  user-select: none;
}

.role-tab.active {
  background: #409EFF;
  color: #fff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.role-tab:hover:not(.active) {
  color: #409EFF;
  background: rgba(64, 158, 255, 0.08);
}

.login-form {
  font-size: 18px;
}

.login-form :deep(.el-input__wrapper) {
  font-size: 18px;
}

.login-form :deep(.el-input__inner) {
  font-size: 18px;
  height: 48px;
  line-height: 48px;
}

.login-form :deep(.el-input__prefix) {
  font-size: 18px;
}

.slider-row {
  width: 100%;
}
.slider-track {
  display: flex;
  align-items: center;
  gap: 8px;
}
.slider-min, .slider-max {
  font-size: 16px;
  color: #999;
  flex-shrink: 0;
}
.slider-main {
  flex: 1;
}

.captcha-row { display: flex; gap: 12px; width: 100%; }
.captcha-input { flex: 1; }
.captcha-img { width: 280px; height: 90px; border-radius: 8px; overflow: hidden; cursor: pointer; border: 2px solid #e4e7ed; flex-shrink: 0; display: flex; align-items: center; justify-content: center; background: #fafafa; }
.captcha-img img { width: 100%; height: 100%; object-fit: cover; }
.captcha-placeholder { font-size: 14px; color: #c0c4cc; }

.login-btn {
  width: 100%;
  height: 52px;
  font-size: 20px;
  font-weight: 600;
  border-radius: 10px;
  letter-spacing: 4px;
}

.form-footer {
  text-align: center;
  margin-top: -8px;
}

.register-link {
  font-size: 16px;
  color: #909399;
  cursor: pointer;
  user-select: none;
}

.register-link .link-text {
  color: #409EFF;
  font-weight: 500;
}

.register-link:hover .link-text {
  text-decoration: underline;
}
</style>