<template>
  <div class="escort-apply">
    <div class="apply-header">
      <h2>陪诊员资质认证</h2>
      <p>请如实填写以下信息，提交后等待管理员审核</p>
    </div>

    <el-card shadow="never">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" maxlength="20" />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio value="MALE">男</el-radio>
            <el-radio value="FEMALE">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker
            v-model="form.birthDate"
            type="date"
            placeholder="选择出生日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" maxlength="18" />
        </el-form-item>

        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
        </el-form-item>

        <el-form-item label="服务城市" prop="serviceCity">
          <el-input v-model="form.serviceCity" placeholder="例如：北京市" />
        </el-form-item>

        <el-form-item label="服务区域" prop="serviceArea">
          <el-input v-model="form.serviceArea" placeholder="例如：朝阳区、海淀区" />
        </el-form-item>

        <el-form-item label="个人介绍" prop="intro">
          <el-input
            v-model="form.intro"
            type="textarea"
            :rows="4"
            placeholder="请介绍您的工作经验和擅长领域"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="资质材料" prop="materials">
          <el-upload
            v-model:file-list="fileList"
            :action="uploadUrl"
            list-type="picture-card"
            multiple
            :on-success="onUploadSuccess"
            :on-remove="onRemove"
            :before-upload="beforeUpload"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">支持 jpg/png 格式，单张不超过 5MB</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitForm">
            提交认证
          </el-button>
          <el-button @click="$router.push('/escort/home')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '@/api/request'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const fileList = ref([])
const uploadUrl = '/file/upload'

const form = reactive({
  realName: '',
  gender: 'MALE',
  birthDate: '',
  idCard: '',
  phone: '',
  serviceCity: '',
  serviceArea: '',
  intro: '',
  materials: ''
})

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  birthDate: [{ required: true, message: '请选择出生日期', trigger: 'change' }],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '身份证号格式不正确', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  serviceCity: [{ required: true, message: '请输入服务城市', trigger: 'blur' }],
  serviceArea: [{ required: true, message: '请输入服务区域', trigger: 'blur' }],
  intro: [{ required: true, message: '请输入个人介绍', trigger: 'blur' }]
}

function onUploadSuccess(response) {
  if (response?.data?.url) {
    const urls = form.materials ? form.materials.split(',') : []
    urls.push(response.data.url)
    form.materials = urls.join(',')
  }
}

function onRemove(file) {
  const url = file.response?.data?.url || file.url
  if (url) {
    const urls = form.materials.split(',').filter(u => u && u !== url)
    form.materials = urls.join(',')
  }
}

function beforeUpload(file) {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG 格式的图片')
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
  }
  return isImage && isLt5M
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await api.post('/escort/apply', form)
    ElMessage.success('资质认证提交成功')
    router.push('/escort/home')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.escort-apply {
  padding: 20px;
  max-width: 700px;
  margin: 0 auto;
}
.apply-header {
  margin-bottom: 24px;
}
.apply-header h2 {
  margin: 0 0 8px 0;
  font-size: 22px;
}
.apply-header p {
  margin: 0;
  color: #999;
  font-size: 14px;
}
.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}
</style>