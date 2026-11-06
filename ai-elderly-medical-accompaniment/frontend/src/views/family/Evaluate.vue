<template>
  <div class="evaluate-page">
    <div class="page-header">
      <el-button icon="ArrowLeft" text @click="$router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">服务评价</h1>
      <div style="width: 32px;"></div>
    </div>

    <div class="order-summary" v-if="order">
      <h2 class="summary-title">订单概况</h2>
      <div class="summary-item">
        <span class="label">订单编号</span>
        <span class="value">{{ order.orderNo }}</span>
      </div>
      <div class="summary-item">
        <span class="label">陪诊老人</span>
        <span class="value">{{ order.elderlyName }}</span>
      </div>
      <div class="summary-item">
        <span class="label">就诊医院</span>
        <span class="value">{{ order.hospitalName }}</span>
      </div>
      <div class="summary-item">
        <span class="label">就诊科室</span>
        <span class="value">{{ order.department }}</span>
      </div>
      <div class="summary-item">
        <span class="label">预约时间</span>
        <span class="value">{{ order.appointmentTime }}</span>
      </div>
    </div>

    <div class="evaluate-card">
      <h2 class="eval-title">您的评价</h2>

      <div class="rate-row">
        <span class="rate-label">服务评分</span>
        <el-rate
          v-model="score"
          :max="5"
          :size="32"
          :texts="rateTexts"
          show-text
        />
      </div>

      <div class="comment-row">
        <span class="comment-label">评价内容</span>
        <el-input
          v-model="content"
          type="textarea"
          :rows="5"
          placeholder="请输入您的评价，帮助我们做得更好..."
          maxlength="500"
          show-word-limit
        />
      </div>

      <el-button
        type="primary"
        class="submit-btn"
        :loading="submitting"
        @click="handleSubmit"
      >
        提交评价
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api/request'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const score = ref(0)
const content = ref('')
const submitting = ref(false)

const rateTexts = ['非常差', '差', '一般', '好', '非常好']

const loadOrder = async () => {
  try {
    const res = await api.get(`/order/${route.params.id}`)
    if (res.code === 200 && res.data) {
      order.value = res.data
    }
  } catch (e) {
    ElMessage.error('加载订单信息失败')
  }
}

const handleSubmit = async () => {
  if (score.value === 0) {
    ElMessage.warning('请为服务评分')
    return
  }
  submitting.value = true
  try {
    const res = await api.post(`/order/${route.params.id}/evaluate`, {
      score: score.value,
      content: content.value
    })
    if (res.code === 200) {
      ElMessage.success('评价提交成功，感谢您的反馈')
      router.push('/family/orders')
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadOrder)
</script>

<style scoped>
.evaluate-page {
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

.order-summary {
  margin: 16px 20px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.summary-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 16px 0;
  color: #333;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.summary-item:last-child {
  border-bottom: none;
}

.summary-item .label {
  font-size: 18px;
  color: #666;
}

.summary-item .value {
  font-size: 18px;
  color: #333;
  font-weight: 500;
}

.evaluate-card {
  margin: 0 20px;
  background: #fff;
  border-radius: 12px;
  padding: 24px 20px;
}

.eval-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 24px 0;
  color: #333;
  text-align: center;
}

.rate-row {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.rate-label {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.comment-row {
  margin-bottom: 24px;
}

.comment-label {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 10px;
}

.comment-row :deep(.el-textarea__inner) {
  font-size: 18px;
}

.submit-btn {
  width: 100%;
  height: 52px;
  font-size: 22px;
  background: #2F5AA8;
  border-color: #2F5AA8;
  border-radius: 12px;
}

:deep(.el-rate__text) {
  font-size: 18px;
}
</style>