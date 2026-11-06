<template>
  <div class="register-container">
    <div class="register-card">
      <!-- Header Area -->
      <div class="header-area">
        <h1 class="register-title">创建账号</h1>
        <p class="register-subtitle">加入AI赋能陪诊助医服务系统</p>
      </div>

      <!-- Register Form -->
      <el-form
        ref="formRef"
        :model="registerForm"
        :rules="rules"
        class="register-form"
        size="large"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号"
            :prefix-icon="Phone"
            clearable
          />
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
            v-model="registerForm.nickname"
            placeholder="请输入昵称"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码（6-30位）"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item prop="role">
          <el-select
            v-model="registerForm.role"
            placeholder="请选择角色"
            class="role-select"
          >
            <el-option label="家属端 — 为家中老人预约陪诊" value="FAMILY" />
            <el-option label="陪诊师端 — 提供陪诊服务（注册后需提交资质审核）" value="ESCORT" />
          </el-select>
          <div v-if="registerForm.role === 'FAMILY'" class="role-hint">
            为家中老人预约陪诊服务，管理老人档案与就医记录
          </div>
          <div v-if="registerForm.role === 'ESCORT'" class="role-hint">
            提供陪诊服务，注册后需提交资质审核通过方可接单
          </div>
        </el-form-item>

        <el-form-item prop="captcha">
          <div class="captcha-row">
            <el-input v-model="registerForm.captcha" placeholder="4位数字验证码" clearable class="captcha-input" />
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
            class="register-btn"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form-item>

        <div class="form-footer">
          <span class="login-link" @click="$router.push('/login')">
            已有账号？<span class="link-text">立即登录</span>
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
import { Phone, User, Lock } from '@element-plus/icons-vue'
import request from '@/api/request'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const captchaSvg = ref('')
const captchaKey = ref('')

const registerForm = reactive({
  phone: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  role: '',
  captcha: ''
})

const validateConfirmPassword = (_rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度在 6 到 30 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
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

const handleRegister = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await request.post('/auth/register', {
      username: registerForm.phone,
      nickname: registerForm.nickname,
      password: registerForm.password,
      role: registerForm.role,
      captcha: registerForm.captcha,
      captchaKey: captchaKey.value
    })

    ElMessage.success(res.data?.message || '注册成功，请登录')
    router.push('/login')
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || '注册失败，请重试'
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
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f4ff 0%, #f0f7ff 50%, #e6f0fa 100%);
  padding: 20px;
}

.register-card {
  width: 100%;
  max-width: 440px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(64, 158, 255, 0.12);
  padding: 48px 40px 40px;
}

.header-area {
  text-align: center;
  margin-bottom: 32px;
}

.register-title {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 8px 0;
  letter-spacing: 1px;
}

.register-subtitle {
  font-size: 15px;
  color: #909399;
  margin: 0;
}

.register-form {
  font-size: 18px;
}

.register-form :deep(.el-input__wrapper) {
  font-size: 18px;
}

.register-form :deep(.el-input__inner) {
  font-size: 18px;
  height: 48px;
  line-height: 48px;
}

.register-form :deep(.el-input__prefix) {
  font-size: 18px;
}

.role-select {
  width: 100%;
}

.role-select :deep(.el-input__wrapper) {
  font-size: 18px;
}

.captcha-row { display: flex; gap: 12px; width: 100%; }
.captcha-input { flex: 1; }
.captcha-img { width: 280px; height: 90px; border-radius: 8px; overflow: hidden; cursor: pointer; border: 2px solid #e4e7ed; flex-shrink: 0; display: flex; align-items: center; justify-content: center; background: #fafafa; }
.captcha-img img { width: 100%; height: 100%; object-fit: cover; }
.captcha-placeholder { font-size: 14px; color: #c0c4cc; }

.role-select :deep(.el-input__inner) {
  font-size: 18px;
  height: 48px;
  line-height: 48px;
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

.register-btn {
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

.login-link {
  font-size: 16px;
  color: #909399;
  cursor: pointer;
  user-select: none;
}

.login-link .link-text {
  color: #409EFF;
  font-weight: 500;
}

.login-link:hover .link-text {
  text-decoration: underline;
}

.role-hint {
  font-size: 16px;
  color: #909399;
  margin-top: 6px;
  line-height: 1.4;
}
</style>