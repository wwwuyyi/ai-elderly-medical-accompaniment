<!-- 三端共享个人信息与修改密码页 -->
<template>
  <div class="profile-page">
    <div class="header-bar">
      <el-button @click="goHome"><ArrowLeft /> 返回</el-button>
      <h2>个人中心</h2>
      <div style="width:60px"></div>
    </div>

    <div v-if="loading" style="padding:40px;text-align:center">
      <el-icon :size="32" class="is-loading"><Loading /></el-icon>
    </div>

    <template v-else>
      <!-- 基本信息卡片 -->
      <el-card class="info-card">
        <template #header><span style="font-size:18px;font-weight:700">基本信息</span></template>
        <el-form :model="form" label-width="100px" size="large">
          <el-form-item label="手机号">
            <span class="readonly">{{ profile.username }}</span>
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="角色">
            <el-tag size="large">{{ roleLabel(profile.role) }}</el-tag>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveProfile" style="height:48px;font-size:20px">保存</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 修改密码卡片 -->
      <el-card class="pwd-card">
        <template #header><span style="font-size:18px;font-weight:700">修改密码</span></template>
        <el-form :model="pwd" :rules="pwdRules" ref="pwdRef" label-width="100px" size="large">
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input v-model="pwd.oldPassword" type="password" show-password placeholder="请输入旧密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwd.newPassword" type="password" show-password placeholder="6-20位新密码" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="pwd.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="warning" :loading="changing" @click="changePwd" style="height:48px;font-size:20px">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import api from '@/api/request'

const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const changing = ref(false)
const pwdRef = ref(null)
const profile = ref({})

const form = reactive({ nickname: '', phone: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (_, v, cb) => v === pwd.newPassword ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' }
  ]
}

function roleLabel(r) {
  return { FAMILY: '家属', ESCORT: '陪诊师', ADMIN: '管理员' }[r] || r
}

function goHome() {
  const r = profile.value.role
  if (r === 'FAMILY') router.push('/family/home')
  else if (r === 'ESCORT') router.push('/escort/home')
  else router.push('/admin/dashboard')
}

async function loadProfile() {
  try {
    const res = await api.get('/user/profile')
    profile.value = res.data || {}
    form.nickname = profile.value.nickname || ''
    form.phone = profile.value.phone || ''
  } catch {
    ElMessage.error('加载个人信息失败')
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  try {
    await api.put('/user/profile', { nickname: form.nickname, phone: form.phone })
    ElMessage.success('个人信息已保存')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function changePwd() {
  if (!pwdRef.value) return
  try { await pwdRef.value.validate() } catch { return }
  changing.value = true
  try {
    await api.put('/user/password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwd.oldPassword = ''; pwd.newPassword = ''; pwd.confirmPassword = ''
    sessionStorage.clear()
    router.push('/login')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '修改失败')
  } finally {
    changing.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page { min-height: 100vh; background: #f5f7fa; padding-bottom: 30px; }
.header-bar { display: flex; justify-content: space-between; align-items: center; padding: 14px 20px; background: #fff; border-bottom: 1px solid #eee; }
.header-bar h2 { margin: 0; font-size: 22px; }
.info-card, .pwd-card { margin: 16px 20px; }
.readonly { font-size: 18px; color: #666; }
</style>