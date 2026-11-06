<template>
  <div class="order-create-page">
    <div class="page-header">
      <el-button icon="ArrowLeft" text @click="$router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">预约陪诊</h1>
      <div style="width: 32px;"></div>
    </div>

    <div class="steps-wrap">
      <el-steps :active="activeStep" align-center>
        <el-step title="选择老人" />
        <el-step title="填写信息" />
        <el-step title="确认下单" />
      </el-steps>
    </div>

    <!-- Step 1: 选择老人 -->
    <div class="step-content" v-if="activeStep === 0">
      <div
        v-for="elderly in elderlyList"
        :key="elderly.id"
        class="elderly-card"
        :class="{ selected: selectedElderly?.id === elderly.id }"
        @click="selectElderly(elderly)"
      >
        <div class="card-left">
          <div class="avatar">{{ elderly.name?.charAt(0) || '老' }}</div>
          <div class="info">
            <span class="name">{{ elderly.name }}</span>
            <span class="mobility">{{ getMobilityLabel(elderly.mobility) }}</span>
          </div>
        </div>
        <el-icon v-if="selectedElderly?.id === elderly.id" :size="24" color="#2F5AA8">
          <SuccessFilled />
        </el-icon>
      </div>
      <el-empty v-if="elderlyList.length === 0" description="请先添加老人档案" :image-size="80">
        <el-button type="primary" size="large" @click="$router.push('/family/elderly/add')" style="height:48px;font-size:20px">去添加老人档案</el-button>
      </el-empty>
    </div>

    <!-- Step 2: 填写信息 -->
    <div class="step-content" v-if="activeStep === 1">
      <el-form ref="formRef" :model="orderForm" :rules="rules" label-position="top" size="large">
        <el-form-item label="就诊医院" prop="hospitalId">
          <el-select v-model="orderForm.hospitalId" placeholder="请选择医院" style="width:100%" size="large">
            <el-option v-for="h in hospitals" :key="h.id" :label="h.name" :value="h.id" /></el-select>
        </el-form-item>

        <el-form-item label="就诊科室" prop="department">
          <el-input v-model="orderForm.department" placeholder="请输入科室名称" />
        </el-form-item>

        <el-form-item label="服务类型" prop="serviceType">
          <el-select v-model="orderForm.serviceType" placeholder="请选择服务类型" style="width: 100%">
            <el-option label="基础陪诊 50元/小时" value="BASIC" />
            <el-option label="陪同检查 80元/小时" value="EXAM" />
          </el-select>
        </el-form-item>

        <el-form-item label="预约时间" prop="appointmentTime">
          <el-date-picker
            v-model="orderForm.appointmentTime"
            type="datetime"
            placeholder="请选择预约时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
            :disabled-date="disabledDate"
          />
        </el-form-item>

        <el-form-item label="服务时长(小时)" prop="duration">
          <el-input-number
            v-model="orderForm.duration"
            :min="1"
            :max="24"
            style="width: 100%"
            placeholder="请输入服务时长"
          />
        </el-form-item>
      </el-form>

      <div class="estimate-box" v-if="estimatedAmount !== null">
        <span class="estimate-label">预估费用</span>
        <span class="estimate-amount">¥{{ estimatedAmount.toFixed(2) }}</span>
      </div>

      <el-button
        type="primary"
        class="estimate-btn"
        @click="handleEstimate"
        :loading="estimating"
      >
        费用试算
      </el-button>
    </div>

    <!-- Step 3: 确认下单 -->
    <div class="step-content" v-if="activeStep === 2">
      <div class="confirm-card">
        <h3 class="confirm-title">订单确认</h3>
        <div class="confirm-item">
          <span class="label">陪诊老人</span>
          <span class="value">{{ selectedElderly?.name }}</span>
        </div>
        <div class="confirm-item">
          <span class="label">就诊医院</span>
          <span class="value">{{ hospitals.find(h=>h.id===orderForm.hospitalId)?.name || '' }}</span>
        </div>
        <div class="confirm-item">
          <span class="label">就诊科室</span>
          <span class="value">{{ orderForm.department }}</span>
        </div>
        <div class="confirm-item">
          <span class="label">服务类型</span>
          <span class="value">{{ orderForm.serviceType === 'BASIC' ? '基础陪诊' : '陪同检查' }}</span>
        </div>
        <div class="confirm-item">
          <span class="label">预约时间</span>
          <span class="value">{{ orderForm.appointmentTime }}</span>
        </div>
        <div class="confirm-item">
          <span class="label">服务时长</span>
          <span class="value">{{ orderForm.duration }}小时</span>
        </div>
        <div class="confirm-divider"></div>
        <div class="confirm-item total">
          <span class="label">预估费用</span>
          <span class="amount">¥{{ estimatedAmount?.toFixed(2) || '0.00' }}</span>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="bottom-bar">
      <el-button
        v-if="activeStep > 0"
        class="nav-btn prev-btn"
        @click="activeStep--"
      >
        上一步
      </el-button>
      <el-button
        v-if="activeStep < 2"
        type="primary"
        class="nav-btn next-btn"
        @click="handleNext"
      >
        下一步
      </el-button>
      <el-button
        v-if="activeStep === 2"
        type="primary"
        class="nav-btn submit-btn"
        :loading="submitting"
        @click="handleSubmit"
      >
        确认下单
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, SuccessFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api/request'

const router = useRouter()

const activeStep = ref(0)
const elderlyList = ref([])
const selectedElderly = ref(null)
const formRef = ref(null)
const estimating = ref(false)
const estimatedAmount = ref(null)
const requestId = ref(crypto.randomUUID())
const submitting = ref(false)
const hospitals = ref([])

const orderForm = ref({
  hospitalId: null,
  department: '',
  serviceType: '',
  appointmentTime: '',
  duration: 1
})

const rules = {
  hospitalId: [{ required: true, message: '请选择就诊医院', trigger: 'change' }],
  department: [{ required: true, message: '请输入就诊科室', trigger: 'blur' }],
  serviceType: [{ required: true, message: '请选择服务类型', trigger: 'change' }],
  appointmentTime: [{ required: true, message: '请选择预约时间', trigger: 'change' }],
  duration: [{ required: true, message: '请输入服务时长', trigger: 'blur' }]
}

const getMobilityLabel = (mobility) => {
  const map = { WALK: '自主行走', ASSIST: '需搀扶', WHEELCHAIR: '轮椅' }
  return map[mobility] || '-'
}

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 86400000
}

// 服务类型/时长变更时清空试算结果，强制重新试算
watch(() => orderForm.value.serviceType, () => { estimatedAmount.value = null })
watch(() => orderForm.value.duration, () => { estimatedAmount.value = null })

const selectElderly = (elderly) => {
  selectedElderly.value = elderly
}

const handleNext = async () => {
  if (activeStep.value === 0) {
    if (!selectedElderly.value) {
      ElMessage.warning('请选择一位老人')
      return
    }
    activeStep.value = 1
  } else if (activeStep.value === 1) {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
    if (estimatedAmount.value === null) {
      ElMessage.warning('请先进行费用试算')
      return
    }
    activeStep.value = 2
  }
}

const handleEstimate = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  estimating.value = true
  try {
    const res = await api.post('/order/estimate', {
      serviceType: orderForm.value.serviceType,
      duration: orderForm.value.duration
    })
    if (res.code === 200 && res.data) {
      estimatedAmount.value = res.data.totalAmount || 0
      ElMessage.success('费用估算完成')
    } else {
      ElMessage.error(res.message || '费用试算失败')
    }
  } catch (e) {
    ElMessage.error('费用试算失败')
  } finally {
    estimating.value = false
  }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const res = await api.post('/order', {
      requestId: requestId.value,
      elderlyId: selectedElderly.value.id,
      hospitalId: orderForm.value.hospitalId,
      department: orderForm.value.department,
      serviceType: orderForm.value.serviceType,
      appointmentTime: orderForm.value.appointmentTime,
      duration: orderForm.value.duration,
      amount: estimatedAmount.value
    })
    if (res.code === 200) {
      ElMessage.success('下单成功')
      router.push('/family/orders')
    } else {
      ElMessage.error(res.message || '下单失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '下单失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const hRes = await api.get('/hospital/list')
    if (hRes.data) hospitals.value = hRes.data
    const res = await api.get('/elderly/page')
    if (res.code === 200 && res.data) {
      elderlyList.value = res.data.records || []
    }
  } catch (e) {
    ElMessage.error('加载老人列表失败')
  }
})
</script>

<style scoped>
.order-create-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 100px;
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

.steps-wrap {
  padding: 20px;
  background: #fff;
  margin-bottom: 12px;
}

.step-content {
  padding: 16px 20px;
}

.elderly-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border: 2px solid #eee;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: border-color 0.3s;
}

.elderly-card.selected {
  border-color: #2F5AA8;
  background: rgba(47, 90, 168, 0.04);
}

.card-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #2F5AA8;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  flex-shrink: 0;
}

.name {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.mobility {
  font-size: 16px;
  color: #999;
  margin-left: 10px;
}

.estimate-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  margin: 16px 0;
}

.estimate-label {
  font-size: 18px;
  color: #666;
}

.estimate-amount {
  font-size: 28px;
  font-weight: 700;
  color: #E6A23C;
}

.estimate-btn {
  width: 100%;
  height: 48px;
  font-size: 20px;
  border-radius: 12px;
  background: #2F5AA8;
  border-color: #2F5AA8;
}

.confirm-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px 20px;
}

.confirm-title {
  font-size: 22px;
  font-weight: 700;
  text-align: center;
  margin: 0 0 20px 0;
  color: #333;
}

.confirm-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
}

.confirm-item .label {
  font-size: 18px;
  color: #666;
}

.confirm-item .value {
  font-size: 18px;
  color: #333;
  font-weight: 500;
}

.confirm-divider {
  border-top: 1px dashed #ddd;
  margin: 8px 0;
}

.confirm-item.total .amount {
  font-size: 28px;
  font-weight: 700;
  color: #E6A23C;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 12px;
  padding: 12px 20px calc(12px + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid #eee;
  z-index: 100;
}

.nav-btn {
  flex: 1;
  height: 52px;
  font-size: 22px;
  border-radius: 12px;
}

.prev-btn {
  background: #f5f5f5;
  border-color: #ddd;
  color: #666;
}

.next-btn,
.submit-btn {
  background: #2F5AA8;
  border-color: #2F5AA8;
}

:deep(.el-step__title) {
  font-size: 16px;
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

:deep(.el-input-number .el-input__inner) {
  font-size: 18px;
}
</style>