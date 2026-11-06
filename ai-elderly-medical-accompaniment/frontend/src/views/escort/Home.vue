<template>
  <div class="escort-home">
    <!-- Loading -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- No profile yet -->
    <el-result
      v-else-if="!statusInfo"
      icon="warning"
      title="请先完成资质认证"
      sub-title="您还未提交陪诊员资质认证，请先完成认证后再使用系统。"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/escort/apply')">
          去认证
        </el-button>
      </template>
    </el-result>

    <!-- PENDING -->
    <el-result
      v-else-if="statusInfo.auditStatus === 'PENDING'"
      icon="info"
      title="资质审核中"
      sub-title="您的资质正在审核中，请耐心等待。审核通过后即可开始接单。"
    >
      <template #extra>
        <el-button type="primary" plain disabled>审核中...</el-button>
      </template>
    </el-result>

    <!-- REJECTED -->
    <el-result
      v-else-if="statusInfo.auditStatus === 'REJECTED'"
      icon="error"
      title="资质审核未通过"
    >
      <template #sub-title>
        <span style="color: #F56C6C;">{{ statusInfo.auditRemark || '您的资质审核未通过，请重新提交认证信息。' }}</span>
      </template>
      <template #extra>
        <el-button type="primary" @click="$router.push('/escort/apply')">
          重新认证
        </el-button>
      </template>
    </el-result>

    <!-- APPROVED -->
    <template v-else-if="statusInfo.auditStatus === 'APPROVED'">
      <!-- Welcome -->
      <div class="welcome-section">
        <div class="welcome-card">
          <el-avatar :size="64" icon="UserFilled" />
          <div class="welcome-info">
            <h2>欢迎回来，{{ statusInfo.realName }}</h2>
            <el-button text @click="handleLogout" style="color:#fff;margin-top:8px;font-size:16px">退出登录</el-button>
            <div class="rating-row">
              <el-rate
                :model-value="statusInfo.avgRating || 0"
                disabled
                show-score
                text-color="#ff9900"
              />
              <span class="rating-text">
                {{ (statusInfo.avgRating || 0).toFixed(1) }} 分
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="quick-actions">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card shadow="hover" class="action-card" @click="$router.push('/escort/pool')">
              <div class="action-content">
                <el-icon :size="48" color="#409EFF"><List /></el-icon>
                <span class="action-label">接单大厅</span>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" class="action-card" @click="$router.push('/escort/orders')">
              <div class="action-content">
                <el-icon :size="48" color="#67C23A"><Document /></el-icon>
                <span class="action-label">我的订单</span>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" class="action-card" @click="$router.push('/escort/statistics')">
              <div class="action-content">
                <el-icon :size="48" color="#E6A23C"><TrendCharts /></el-icon>
                <span class="action-label">收入统计</span>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- Recent Notifications -->
      <div class="notifications-section" v-if="notifications.length > 0">
        <h3>近期订单通知</h3>
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          @click="$router.push(`/escort/order/${item.orderId}`)"
        >
          <el-icon><Bell /></el-icon>
          <span>{{ item.content }}</span>
          <span class="notify-time">{{ item.createTime }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import api from '@/api/request'
import { List, Document, TrendCharts, Bell } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const statusInfo = ref(null)
const notifications = ref([])

onMounted(async () => {
  try {
    const res = await api.get('/escort/apply/status')
    statusInfo.value = res.data
  } catch (e) {
    // statusInfo remains null → show "请先完成资质认证"
    statusInfo.value = null
  } finally {
    loading.value = false
  }

  // Try to load notifications
  try {
    const notiRes = await api.get('/order/page', { params: { page: 1, size: 10 } })
    if (notiRes.data?.records) {
      notifications.value = notiRes.data.records.map(r => ({
        id: r.id,
        orderId: r.id,
        content: `订单 ${r.orderNo} - ${r.elderlyName} - ${r.appointmentTime}`,
        createTime: r.createTime
      })).slice(0, 5)
    }
  } catch (e) {
    notifications.value = []
  }
})

const handleLogout = () => { userStore.logout(); router.push('/login') }
</script>

<style scoped>
.escort-home {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}
.loading-container {
  padding: 40px;
}
.welcome-section {
  margin-bottom: 30px;
}
.welcome-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background: linear-gradient(135deg, #409EFF, #67C23A);
  border-radius: 12px;
  color: #fff;
}
.welcome-card h2 {
  margin: 0 0 8px 0;
  font-size: 22px;
}
.rating-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.rating-text {
  font-size: 14px;
  opacity: 0.9;
}
.quick-actions {
  margin-bottom: 30px;
}
.action-card {
  cursor: pointer;
  text-align: center;
  transition: transform 0.2s;
}
.action-card:hover {
  transform: translateY(-2px);
}
.action-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
}
.action-label {
  font-size: 16px;
  font-weight: 500;
}
.notifications-section {
  margin-top: 20px;
}
.notifications-section h3 {
  margin-bottom: 12px;
  font-size: 16px;
}
.notification-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.notification-item:hover {
  background: #ecf5ff;
}
.notify-time {
  margin-left: auto;
  font-size: 12px;
  color: #999;
}
</style>