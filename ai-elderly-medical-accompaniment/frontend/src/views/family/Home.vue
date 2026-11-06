<template>
  <div class="family-home">
    <div class="header">
      <h1 class="app-title">陪诊助医服务</h1>
      <div style="display:flex;align-items:center;gap:16px">
        <el-badge :value="notificationCount" :hidden="notificationCount === 0" class="notification-badge" @click="$router.push('/family/notify')">
          <el-icon :size="24"><Bell /></el-icon>
        </el-badge>
        <el-button text @click="handleLogout" style="color:#fff;font-size:18px;height:48px">退出</el-button>
      </div>
    </div>

    <div class="main-card">
      <div class="card-icon">
        <el-icon :size="48"><Service /></el-icon>
      </div>
      <p class="card-title">为家人预约陪诊服务</p>
      <p class="card-desc">专业陪诊师全程陪同，让家人就医更省心</p>
      <el-button
        type="primary"
        class="order-btn"
        @click="$router.push('/family/order/create')"
      >
        立即下单
      </el-button>
    </div>

    <div class="section" v-if="recentOrders.length > 0">
      <div class="section-header">
        <h2 class="section-title">最近订单</h2>
        <span class="section-more" @click="$router.push('/family/orders')">查看全部 ></span>
      </div>
      <div
        v-for="order in recentOrders"
        :key="order.id"
        class="order-item"
        @click="$router.push(`/family/order/${order.id}`)"
      >
        <div class="order-left">
          <span class="order-no">{{ order.orderNo }}</span>
          <span class="order-elderly">{{ order.elderlyName }}</span>
        </div>
        <div class="order-right">
          <el-tag :type="getStatusType(order.status)" size="large">{{ getStatusLabel(order.status) }}</el-tag>
          <span class="order-time">{{ order.appointmentTime }}</span>
        </div>
      </div>
    </div>

    <div class="section" v-else>
      <el-empty description="暂无订单" :image-size="80" />
    </div>

    <div class="bottom-nav">
      <div class="nav-item" @click="$router.push('/family/elderly')">
        <el-icon :size="22"><User /></el-icon>
        <span>老人档案</span>
      </div>
      <div class="nav-item" @click="$router.push('/family/orders')">
        <el-icon :size="22"><Document /></el-icon>
        <span>我的订单</span>
      </div>
      <div class="nav-item" @click="$router.push('/family/records')">
        <el-icon :size="22"><Folder /></el-icon>
        <span>就医档案</span>
      </div>
      <div class="nav-item nav-item-ai" @click="$router.push('/family/ai-chat')">
        <el-icon :size="22"><ChatDotRound /></el-icon>
        <span>AI助手</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Bell, Service, User, Document, Folder, ChatDotRound } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import api from '@/api/request'

const router = useRouter()
const userStore = useUserStore()
const notificationCount = ref(0)
const recentOrders = ref([])

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

const handleLogout = async () => {
    await userStore.logout()
    router.push('/login')
  }

onMounted(async () => {
  try {
    const [orderRes, notifyRes] = await Promise.all([
      api.get('/order/page', { params: { current: 1, size: 5 } }),
      api.get('/notification/page', { params: { current: 1, size: 1 } })
    ])
    if (orderRes.code === 200 && orderRes.data) {
      recentOrders.value = orderRes.data.records || []
    }
    if (notifyRes.code === 200 && notifyRes.data) {
      notificationCount.value = notifyRes.data.total || 0
    }
  } catch (e) {
    console.error('Failed to load home data:', e)
  }
})
</script>

<style scoped>
.family-home {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 80px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #2F5AA8;
  color: #fff;
}

.app-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  color: #fff;
}

.notification-badge {
  cursor: pointer;
  color: #fff;
}

.main-card {
  margin: 20px;
  padding: 32px 24px;
  background: linear-gradient(135deg, #2F5AA8 0%, #4A7BD4 100%);
  border-radius: 16px;
  color: #fff;
  text-align: center;
  box-shadow: 0 4px 12px rgba(47, 90, 168, 0.3);
}

.card-icon {
  margin-bottom: 16px;
  color: rgba(255, 255, 255, 0.9);
}

.card-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 8px 0;
}

.card-desc {
  font-size: 16px;
  margin: 0 0 24px 0;
  opacity: 0.85;
}

.order-btn {
  height: 52px;
  font-size: 22px;
  padding: 0 48px;
  border-radius: 26px;
  background: #fff;
  color: #2F5AA8;
  border: none;
  font-weight: 700;
  letter-spacing: 2px;
}

.order-btn:hover {
  background: #f0f4ff;
  color: #2F5AA8;
}

.section {
  margin: 0 20px 20px;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
  color: #333;
}

.section-more {
  font-size: 18px;
  color: #2F5AA8;
  cursor: pointer;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.order-item:last-child {
  border-bottom: none;
}

.order-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-no {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.order-elderly {
  font-size: 16px;
  color: #666;
}

.order-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.order-time {
  font-size: 16px;
  color: #999;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  background: #fff;
  border-top: 1px solid #eee;
  padding: 10px 0 calc(10px + env(safe-area-inset-bottom));
  z-index: 100;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  padding: 4px 16px;
}

.nav-item:hover,
.nav-item:active {
  color: #2F5AA8;
}

.nav-item-ai {
  color: #2F5AA8;
  font-weight: 600;
}

.el-tag--large {
  font-size: 16px;
  padding: 4px 12px;
}
</style>