<template>
  <div class="order-pool">
    <div class="pool-header">
      <el-button @click="$router.push('/escort/home')">← 返回首页</el-button>
      <h2>接单大厅</h2>
      <el-button @click="fetchPool" :loading="loading">刷新</el-button>
    </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>

    <el-empty v-else-if="orders.length === 0" description="暂无待接订单" />

    <div class="order-list" v-else>
      <el-card
        v-for="order in orders"
        :key="order.id"
        shadow="hover"
        class="order-card"
      >
        <div class="order-info">
          <div class="order-row">
            <span class="label">订单编号</span>
            <span class="value">{{ order.orderNo }}</span>
          </div>
          <div class="order-row">
            <span class="label">就诊人</span>
            <span class="value">{{ desensitize(order.elderlyName) }}</span>
          </div>
          <div class="order-row">
            <span class="label">预约时间</span>
            <span class="value">{{ order.appointmentTime }}</span>
          </div>
          <div class="order-row">
            <span class="label">就诊科室</span>
            <span class="value">{{ order.department }}</span>
          </div>
          <div class="order-row">
            <span class="label">就诊医院</span>
            <span class="value">{{ order.hospital }}</span>
          </div>
        </div>
        <div class="order-actions">
          <el-button
            type="primary"
            :loading="acceptingId === order.id"
            @click="handleAccept(order)"
          >
            接单
          </el-button>
          <el-button
            type="danger"
            plain
            :loading="rejectingId === order.id"
            @click="handleReject(order)"
          >
            拒绝
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api/request'

const loading = ref(false)
const orders = ref([])
const acceptingId = ref(null)
const rejectingId = ref(null)
let pollTimer = null

function desensitize(name) {
  if (!name) return ''
  if (name.length <= 2) return name[0] + '*'
  return name[0] + '*'.repeat(name.length - 2) + name[name.length - 1]
}

async function fetchPool() {
  loading.value = true
  try {
    const res = await api.get('/escort/order/pool')
    orders.value = res.data || []
  } catch (e) {
    ElMessage.error('获取订单池失败')
    orders.value = []
  } finally {
    loading.value = false
  }
}

async function handleAccept(order) {
  try {
    await ElMessageBox.confirm(`确认接单 "${order.orderNo}" 吗？`, '确认接单', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info'
    })
  } catch {
    return
  }
  acceptingId.value = order.id
  try {
    await api.post(`/order/${order.id}/accept`)
    ElMessage.success('接单成功')
    await fetchPool()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '接单失败')
  } finally {
    acceptingId.value = null
  }
}

async function handleReject(order) {
  try {
    await ElMessageBox.confirm(`确认拒绝订单 "${order.orderNo}" 吗？`, '确认拒绝', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  rejectingId.value = order.id
  try {
    await api.post(`/order/${order.id}/reject`)
    ElMessage.success('已拒绝')
    await fetchPool()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    rejectingId.value = null
  }
}

onMounted(() => {
  fetchPool()
  pollTimer = setInterval(fetchPool, 5000) // 每5秒自动刷新接单大厅
})

onUnmounted(() => {
  clearInterval(pollTimer)
})
</script>

<style scoped>
.order-pool {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}
.pool-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.pool-header h2 {
  margin: 0;
  font-size: 20px;
}
.loading-container {
  padding: 20px;
}
.order-card {
  margin-bottom: 16px;
}
.order-card :deep(.el-card__body) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-info {
  flex: 1;
}
.order-row {
  display: flex;
  margin-bottom: 6px;
}
.order-row .label {
  color: #999;
  width: 80px;
  flex-shrink: 0;
}
.order-row .value {
  color: #333;
}
.order-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-left: 20px;
}
</style>