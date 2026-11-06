<template>
  <div class="orders-page">
    <div class="page-header">
      <el-button icon="ArrowLeft" text @click="$router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">我的订单</h1>
      <div style="width: 32px;"></div>
    </div>

    <div class="filter-tabs">
      <el-tabs v-model="activeStatus" @tab-change="handleStatusChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待派单" name="PENDING_DISPATCH" />
        <el-tab-pane label="已派单" name="DISPATCHED" />
        <el-tab-pane label="已接单" name="ACCEPTED" />
        <el-tab-pane label="服务中" name="IN_SERVICE" />
        <el-tab-pane label="服务完成" name="SERVICE_FINISHED" />
        <el-tab-pane label="待结算" name="SETTLED" />
        <el-tab-pane label="已完成" name="COMPLETED" />
        <el-tab-pane label="已取消" name="CANCELLED" />
      </el-tabs>
    </div>

    <div class="order-list" v-if="orders.length > 0">
      <div
        v-for="order in orders"
        :key="order.id"
        class="order-card"
        @click="$router.push(`/family/order/${order.id}`)"
      >
        <div class="order-top">
          <span class="order-no">{{ order.orderNo }}</span>
          <el-tag :type="getStatusType(order.status)" size="large" class="status-tag">
            {{ getStatusLabel(order.status) }}
          </el-tag>
        </div>
        <div class="order-body">
          <div class="order-info">
            <span class="info-item">就诊人：{{ order.elderlyName }}</span>
            <span class="info-item">科室：{{ order.department }}</span>
          </div>
          <div class="order-bottom">
            <span class="appointment-time">
              <el-icon :size="16"><Clock /></el-icon>
              {{ order.appointmentTime }}
            </span>
            <span class="amount" v-if="order.amount">¥{{ order.amount }}</span>
          </div>
        </div>
      </div>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNo"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchOrders"
          :pager-count="5"
        />
      </div>
    </div>

    <el-empty v-else description="暂无订单" :image-size="80" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Clock, ArrowLeft } from '@element-plus/icons-vue'
import api from '@/api/request'

const activeStatus = ref('all')
const orders = ref([])
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)

const getStatusType = (status) => {
  const map = {
    PENDING_DISPATCH: 'warning',
    DISPATCHED: '',
    ACCEPTED: 'success',
    IN_SERVICE: '',
    SERVICE_FINISHED: 'success',
    SETTLED: 'info',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }
  return map[status] || 'info'
}

const getStatusLabel = (status) => {
  const map = {
    PENDING_DISPATCH: '待派单',
    DISPATCHED: '已派单',
    ACCEPTED: '已接单',
    IN_SERVICE: '服务中',
    SERVICE_FINISHED: '服务完成',
    SETTLED: '待结算',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

const handleStatusChange = () => {
  pageNo.value = 1
  fetchOrders()
}

const fetchOrders = async () => {
  try {
    const params = { current: pageNo.value, size: pageSize.value }
    if (activeStatus.value !== 'all') {
      params.status = activeStatus.value
    }
    const res = await api.get('/order/page', { params })
    if (res.code === 200 && res.data) {
      orders.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('Failed to load orders:', e)
  }
}

onMounted(fetchOrders)
let poll = setInterval(fetchOrders, 5000)
onUnmounted(() => clearInterval(poll))
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 20px;
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

.filter-tabs {
  background: #fff;
  padding: 0 8px;
  margin-bottom: 12px;
}

.order-list {
  padding: 0 20px;
}

.order-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.2s;
}

.order-card:active {
  transform: scale(0.98);
}

.order-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.order-no {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.status-tag {
  font-size: 16px;
}

.order-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item {
  font-size: 18px;
  color: #555;
}

.order-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

.appointment-time {
  font-size: 18px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
}

.amount {
  font-size: 22px;
  font-weight: 700;
  color: #E6A23C;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

:deep(.el-tabs__item) {
  font-size: 18px;
}

:deep(.el-tag--large) {
  font-size: 16px;
}
</style>