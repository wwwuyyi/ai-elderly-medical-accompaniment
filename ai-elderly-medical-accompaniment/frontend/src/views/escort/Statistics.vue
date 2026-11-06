<template>
  <div class="escort-statistics">
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:24px">
    <el-button @click="$router.push('/escort/home')">← 返回首页</el-button>
    <h2 style="margin:0">收入统计</h2>
  </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="4" animated />
    </div>

    <!-- Cards -->
    <el-row :gutter="20" v-else>
      <el-col :span="12">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #ecf5ff;">
              <el-icon :size="36" color="#409EFF"><Finished /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.totalOrders || 0 }}</div>
              <div class="stat-label">累计完成订单</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #fef0f0;">
              <el-icon :size="36" color="#F56C6C"><StarFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ (stats.avgRating || 0).toFixed(1) }} <span class="stat-unit">/ 5</span></div>
              <div class="stat-label">平均评分</div>
              <el-rate
                :model-value="stats.avgRating || 0"
                disabled
                show-score
                text-color="#ff9900"
                size="small"
              />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Service City Badge -->
    <el-card shadow="never" class="city-card" v-if="profile">
      <template #header><span>服务信息</span></template>
      <div class="city-info">
        <span class="info-label">服务城市:</span>
        <el-tag type="primary" size="large">{{ profile.serviceCity }}</el-tag>
        <span v-if="profile.serviceArea" style="margin-left: 12px;">
          <span class="info-label">服务区域:</span>
          <el-tag type="success" size="large">{{ profile.serviceArea }}</el-tag>
        </span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Finished, StarFilled } from '@element-plus/icons-vue'
import api from '@/api/request'

const loading = ref(true)
const stats = ref({})
const profile = ref(null)

onMounted(async () => {
  try {
    const [statsRes, profileRes] = await Promise.all([
      api.get('/escort/statistics'),
      api.get('/escort/profile')
    ])
    stats.value = statsRes.data || {}
    profile.value = profileRes.data || null
  } catch (e) {
    stats.value = {}
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.escort-statistics {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}
.loading-container {
  padding: 20px;
}
.stat-card {
  margin-bottom: 20px;
}
.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
}
.stat-icon {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}
.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
}
.stat-unit {
  font-size: 16px;
  font-weight: 400;
  color: #999;
}
.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
}
.city-card {
  margin-top: 20px;
}
.city-info {
  display: flex;
  align-items: center;
}
.info-label {
  color: #666;
  margin-right: 8px;
}
</style>