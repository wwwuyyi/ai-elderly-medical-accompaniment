<template>
  <div class="elderly-edit-page">
    <div class="page-header">
      <el-button icon="ArrowLeft" text @click="$router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">{{ isEdit ? '编辑老人档案' : '新增老人档案' }}</h1>
      <div style="width: 32px;"></div>
    </div>

    <div class="form-wrap">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
      >
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入老人姓名" />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio value="MALE" class="gender-radio">男</el-radio>
            <el-radio value="FEMALE" class="gender-radio">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker
            v-model="form.birthDate"
            type="date"
            placeholder="请选择出生日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" />
        </el-form-item>

        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>

        <el-form-item label="居住地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入居住地址" />
        </el-form-item>

        <el-form-item label="紧急联系人" prop="emergencyContact">
          <el-input v-model="form.emergencyContact" placeholder="请输入紧急联系人电话" />
        </el-form-item>

        <el-form-item label="紧急联系人姓名" prop="emergencyName">
          <el-input v-model="form.emergencyName" placeholder="请输入紧急联系人姓名" />
        </el-form-item>

        <el-form-item label="行动能力" prop="mobility">
          <el-select v-model="form.mobility" placeholder="请选择行动能力" style="width: 100%">
            <el-option label="自主行走" value="WALK" />
            <el-option label="需搀扶" value="ASSIST" />
            <el-option label="轮椅" value="WHEELCHAIR" />
          </el-select>
        </el-form-item>

        <el-form-item label="过敏史" prop="allergyHistory">
          <el-input
            v-model="form.allergyHistory"
            type="textarea"
            :rows="3"
            placeholder="请输入过敏史，如无请填'无'"
          />
        </el-form-item>

        <el-form-item label="慢性病" prop="chronicDisease">
          <el-input
            v-model="form.chronicDisease"
            type="textarea"
            :rows="3"
            placeholder="请输入慢性病情况，如无请填'无'"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="submitting"
            @click="handleSubmit"
          >
            {{ isEdit ? '保存修改' : '确认添加' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api/request'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const formRef = ref(null)
const submitting = ref(false)

const form = ref({
  name: '',
  gender: 'MALE',
  birthDate: '',
  idCard: '',
  phone: '',
  address: '',
  emergencyContact: '',
  emergencyName: '',
  mobility: '',
  allergyHistory: '',
  chronicDisease: ''
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  birthDate: [{ required: true, message: '请选择出生日期', trigger: 'change' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  mobility: [{ required: true, message: '请选择行动能力', trigger: 'change' }]
}

const loadElderly = async () => {
  try {
    const res = await api.get(`/elderly/${route.params.id}`)
    if (res.code === 200 && res.data) {
      const d = res.data
      form.value = {
        name: d.name || '',
        gender: d.gender || 'MALE',
        birthDate: d.birthDate || '',
        idCard: d.idCard || '',
        phone: d.phone || '',
        address: d.address || '',
        emergencyContact: d.emergencyContact || '',
        emergencyName: d.emergencyName || '',
        mobility: d.mobility || '',
        allergyHistory: d.allergyHistory || '',
        chronicDisease: d.chronicDisease || ''
      }
    }
  } catch (e) {
    ElMessage.error('加载老人信息失败')
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    let res
    if (isEdit.value) {
      res = await api.put(`/elderly/${route.params.id}`, form.value)
    } else {
      res = await api.post('/elderly', form.value)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
      router.back()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (isEdit.value) {
    loadElderly()
  }
})
</script>

<style scoped>
.elderly-edit-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 30px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  color: #333;
}

.form-wrap {
  padding: 20px;
}

.gender-radio {
  font-size: 18px;
}

.submit-btn {
  width: 100%;
  height: 52px;
  font-size: 22px;
  background: #2F5AA8;
  border-color: #2F5AA8;
  border-radius: 12px;
  margin-top: 10px;
}

:deep(.el-form-item__label) {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

:deep(.el-input__inner) {
  font-size: 18px;
}

:deep(.el-select .el-input__inner) {
  font-size: 18px;
}

:deep(.el-textarea__inner) {
  font-size: 18px;
}

:deep(.el-radio__label) {
  font-size: 18px;
}
</style>